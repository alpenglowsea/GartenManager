#!/usr/bin/env python3
"""Erzeugt aus Pflanzenliste und rohdaten/ die Textdateien daten/pflanzen/*.json.

Aufruf (im Hauptordner des Projekts):   python3 tools/erzeuge_pflanzen.py

Regeln (siehe DATENMODELL.md, Abschnitte 2.2, 4.1, 4.2):
- Hauptname = Name aus unserer Pflanzenliste. Weitere Namen aus Wikidata/Wikipedia; lateinisch
  aussehende Namen werden als lateinisches Synonym eingeordnet.
- Ein Wikipedia-Abschnitt wird nur übernommen, wenn seine Überschrift zu GENAU EINEM unserer
  Abschnitte passt (Ausnahme: Überschriften, die Herkunft UND Standort betreffen, kommen nur in Herkunft). Unklare Überschriften werden weggelassen und im Bericht gezählt.
- Es zählt nur der eigene Text eines Abschnitts mit passender Überschrift (Unterabschnitte ohne Treffer
  werden nicht angehängt). Absätze über Chromosomenzahlen werden weggelassen.
- Texte werden als Auszug von etwa 1.000 Zeichen aus Originalsätzen übernommen (ganze Absätze;
  ist schon der erste länger, wird am Satzende geschnitten).
- Bestehende, schon echte Pflanzendateien werden NICHT überschrieben (damit Handkorrekturen bleiben),
  ausser mit --neu. Testpflanzen aus Teilschritt 4a werden immer ersetzt.

Ausgabe: daten/pflanzen/*.json, daten/quellen.json, rohdaten/bericht.md, rohdaten/stichprobe.md
"""
import argparse
import csv
import json
import os
import random
import re
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pflanzenliste import WURZEL, finde_liste, lies_liste, slug  # noqa: E402
from baue_grunddaten import normalisiere  # noqa: E402

LIMIT = 1000
ABSCHNITTE = ["Herkunft", "Standort", "Wuchs und Laub", "Blüte", "Verwendung", "Pflege"]
# Schlüsselwörter (klein geschrieben, als Teilstring der Überschrift)
STICHWORTE = {
    "Wuchs und Laub": ["beschreibung", "vegetative merkmale", "erscheinungsbild", "habitus", "wuchs", "blatt", "blätt", "laub", "belaub", "nadeln"],
    "Blüte": ["blüte", "blütenstand", "generative merkmale"],
    "Herkunft": ["vorkommen", "verbreitung", "herkunft", "ursprung"],
    "Standort": ["standort"],
    "Verwendung": ["nutzung", "verwendung", "küche"],
    "Pflege": ["pflege"],
}
# Diese kurzen Stichwörter zählen nur am Wortanfang ("laub" darf nicht in "Aberglauben" oder "Volksglaube" treffen)
WORTANFANG = {"laub", "blatt", "blätt", "wuchs", "nadeln", "belaub"}
AUSSCHLUSS = ["krankheit", "schädling", "parasit", "schaedling", "namensherkunft", "namensgebung", "etymologie", "nutzungsgeschichte"]
ABKUERZUNGEN = {"z", "b", "d", "h", "u", "a", "ca", "bzw", "vgl", "ssp", "var", "subsp", "nr", "etc", "usw", "sog", "bes", "ggf", "evtl", "inkl", "max", "min", "bsp", "dt", "engl", "lat", "syn", "sp", "spp", "ev", "abb", "tab", "vol", "prof", "dr", "hrsg", "jh", "chr", "geb", "gest", "ca"}


def fehler(msg):
    print("FEHLER:", msg)
    sys.exit(1)


# ---------- Wikipedia-Text zerlegen ----------

def zerlege(text):
    """Zerlegt den Klartext in Abschnitte: [{ebene, titel, absaetze}] (Einleitung = Ebene 1, Titel '')."""
    abschnitte = [{"ebene": 1, "titel": "", "absaetze": []}]
    for zeile in text.split("\n"):
        m = re.match(r"^(={2,6})\s*(.+?)\s*\1\s*$", zeile)
        if m:
            abschnitte.append({"ebene": len(m.group(1)), "titel": m.group(2), "absaetze": []})
        elif zeile.strip():
            abschnitte[-1]["absaetze"].append(zeile.strip())
    return abschnitte


def passende(titel):
    """Gibt die Liste unserer Abschnitte zurück, deren Stichwörter in der Überschrift vorkommen."""
    t = titel.lower()
    if any(w in t for w in AUSSCHLUSS):
        return []
    def trifft(w):
        if w in WORTANFANG:
            return re.search(r"(?<![a-zäöüß])" + re.escape(w), t) is not None
        return w in t
    return [a for a in ABSCHNITTE if any(trifft(w) for w in STICHWORTE[a])]


def sortiere(abschnitte, mehrdeutig, nicht_zugeordnet):
    """Ordnet Wikipedia-Abschnitte unseren Abschnitten zu. Gibt {abschnitt: [(titel, [absaetze])]} zurück."""
    ziel = defaultdict(list)
    treffer = [passende(a["titel"]) if a["ebene"] > 1 else None for a in abschnitte]
    for i, a in enumerate(abschnitte):
        if a["ebene"] == 1:
            continue
        gefunden = treffer[i]
        if set(gefunden) == {"Herkunft", "Standort"}:
            # "Verbreitung und Standort": beschreibt das Vorkommen in der Natur, nicht den Gartenstandort
            # (Entscheidung 09.10.2026, ersetzt die vom 03.10.2026) -> nur Herkunft
            gefunden = ["Herkunft"]
        if len(gefunden) > 1:
            mehrdeutig[a["titel"]] += 1
            continue
        if not gefunden:
            if a["ebene"] == 2:
                nicht_zugeordnet[a["titel"]] += 1
            continue
        absaetze = [ab for ab in a["absaetze"] if "chromosom" not in ab.lower()]
        if absaetze:
            for z in gefunden:
                ziel[z].append((a["titel"], absaetze))
    return ziel


# ---------- Auszug ----------

def satzenden(text):
    """Positionen (Ende inklusive Punkt), an denen ein Satz sicher endet."""
    pos = []
    for m in re.finditer(r"[.!?](?=\s+[A-ZÄÖÜ„\"»])", text):
        davor = re.search(r"(\S+)$", text[: m.start()])
        wort = davor.group(1) if davor else ""
        kern = re.sub(r"[^\wäöüß]", "", wort.lower())
        if kern in ABKUERZUNGEN or len(kern) <= 1 or any(c.isdigit() for c in kern):
            continue
        pos.append(m.end())
    return pos


def auszug(absaetze):
    """Gibt (text, gekuerzt) zurück."""
    voll = "\n\n".join(absaetze)
    if len(voll) <= LIMIT:
        return voll, False
    genommen = []
    laenge = 0
    for ab in absaetze:
        zusatz = len(ab) + (2 if genommen else 0)
        if laenge + zusatz <= LIMIT:
            genommen.append(ab)
            laenge += zusatz
        else:
            break
    if genommen:
        return "\n\n".join(genommen), True
    erster = absaetze[0]
    enden = [p for p in satzenden(erster) if p <= LIMIT]
    if enden:
        return erster[: enden[-1]].strip(), True
    # Notfall: kein sicheres Satzende gefunden -> weglassen statt mitten im Satz schneiden
    return "", True


# ---------- Namen ----------

LATEIN = re.compile(r"^[A-Z×][a-zé\-]+( [×x] )?( [a-zé\-\.]+){1,3}$")


def sieht_lateinisch_aus(name, lateinisch):
    teile = name.replace("×", " × ").split()
    if name == lateinisch:
        return True
    if len(teile) >= 2 and teile[0][:1].isupper() and "×" in teile[:2]:
        return True
    return bool(LATEIN.match(name)) and " " in name and name.split()[1][0].islower()


def sammle_namen(p, roh):
    """Weitere Namen zur Pflanze (ausser Hauptname und lateinischem Namen)."""
    namen = []
    gesehen = {normalisiere(p["deutsch"]), normalisiere(p["lateinisch"])}

    def dazu(name, art, quelle):
        name = (name or "").strip()
        if not name or len(name) > 60:
            return
        n = normalisiere(name)
        if n in gesehen:
            return
        gesehen.add(n)
        namen.append({"name": name, "art": art, "quelle": quelle})

    for s in p["synonyme"]:
        dazu(s, "lateinisch", "liste")
    if roh:
        kandidaten = []
        if roh.get("label_de"):
            kandidaten.append((roh["label_de"], "wikidata"))
        kandidaten += [(a, "wikidata") for a in roh.get("aliase_de", [])]
        kandidaten += [(t, "wikidata") for t in roh.get("trivialnamen_de", [])]
        titel = (roh.get("wikipedia") or {}).get("titel") or roh.get("dewiki_titel")
        if titel:
            kandidaten.append((re.sub(r"\s*\(.*?\)\s*$", "", titel), "wikidata"))
        for name, quelle in kandidaten:
            art = "lateinisch" if sieht_lateinisch_aus(name, p["lateinisch"]) else "umgangssprachlich"
            dazu(name, art, quelle)
    return namen


# ---------- Hauptprogramm ----------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--neu", action="store_true", help="auch bestehende echte Pflanzendateien neu erzeugen")
    ap.add_argument("--wurzel", default=WURZEL)
    args = ap.parse_args()
    daten = os.path.join(args.wurzel, "daten")
    pfl_ordner = os.path.join(daten, "pflanzen")
    roh_ordner = os.path.join(args.wurzel, "rohdaten")
    os.makedirs(pfl_ordner, exist_ok=True)

    pflanzen = lies_liste(finde_liste(args.wurzel))
    quellen_pfad = os.path.join(daten, "quellen.json")
    alte_quellen = json.load(open(quellen_pfad, encoding="utf-8")) if os.path.exists(quellen_pfad) else {}
    quellen = {k: v for k, v in alte_quellen.items() if k != "test"}
    quellen["liste"] = {"art": "Projektliste", "titel": "Pflanzenliste des Projektinhabers (Namen)", "adresse": None,
                        "version": None, "lizenz": None, "abrufdatum": None, "aenderung": None}

    statistik = Counter()
    je_gruppe = defaultdict(lambda: Counter())
    mehrdeutig, nicht_zugeordnet = Counter(), Counter()
    ohne_wikidata, ohne_artikel, namensabweichung = [], [], []
    fehlende_roh = []
    abrufdaten = []
    stichprobe_quelle = []
    geschrieben = uebersprungen = 0

    for p in pflanzen:
        ziel_datei = os.path.join(pfl_ordner, p["schluessel"] + ".json")
        roh_datei = os.path.join(roh_ordner, slug(p["lateinisch"]) + ".json")
        roh = json.load(open(roh_datei, encoding="utf-8")) if os.path.exists(roh_datei) else None
        if roh is None:
            fehlende_roh.append(p["schluessel"])
        if os.path.exists(ziel_datei) and not args.neu:
            alt = json.load(open(ziel_datei, encoding="utf-8"))
            ist_test = any(a.get("quelle") == "test" for a in alt.get("angaben", [])) or \
                any(n.get("quelle") == "test" for n in alt.get("namen", []))
            if not ist_test:
                uebersprungen += 1
                continue
        statistik["pflanzen"] += 1
        eintrag = {"hauptname": p["deutsch"], "lateinisch": p["lateinisch"], "gruppen": p["gruppen"],
                   "wikidata": None, "wikipedia": None, "namen": sammle_namen(p, roh),
                   "angaben": [], "pflegehinweise": [], "merkmale": [], "veraltet": False}
        gruppe = p["gruppen"][0]
        je_gruppe[gruppe]["pflanzen"] += 1
        quelle_wp = None
        zugeordnet = {}
        quellen.pop("wp-" + p["schluessel"], None)
        if roh:
            abrufdaten.append(roh.get("abruf", ""))
            if roh.get("wikidata_id"):
                eintrag["wikidata"] = roh["wikidata_id"]
                statistik["mit_wikidata"] += 1
                je_gruppe[gruppe]["wikidata"] += 1
            else:
                ohne_wikidata.append(f"{p['deutsch']} ({p['lateinisch']})")
            wp = roh.get("wikipedia")
            if wp and wp.get("text"):
                statistik["mit_artikel"] += 1
                je_gruppe[gruppe]["artikel"] += 1
                eintrag["wikipedia"] = wp["titel"]
                schluessel_q = "wp-" + p["schluessel"]
                adresse = "https://de.wikipedia.org/w/index.php?title=" + wp["titel"].replace(" ", "_")
                if wp.get("revision"):
                    adresse += f"&oldid={wp['revision']}"
                quellen[schluessel_q] = {
                    "art": "Wikipedia", "titel": f"Wikipedia: {wp['titel']}", "adresse": adresse,
                    "version": str(wp["revision"]) if wp.get("revision") else None,
                    "lizenz": "CC BY-SA 4.0", "abrufdatum": roh.get("abruf"),
                    "aenderung": "Text einem Abschnitt zugeordnet und gegebenenfalls zum Auszug gekürzt",
                }
                quelle_wp = schluessel_q
                sortiert = sortiere(zerlege(wp["text"]), mehrdeutig, nicht_zugeordnet)
                for abschnitt in ABSCHNITTE:
                    teile = sortiert.get(abschnitt)
                    if not teile:
                        continue
                    alle_absaetze = [a for _, abs_ in teile for a in abs_]
                    text, gekuerzt = auszug(alle_absaetze)
                    if not text:
                        statistik["leer_nach_kuerzen"] += 1
                        continue
                    eintrag["angaben"].append({"abschnitt": abschnitt, "text": text, "quelle": quelle_wp, "auszug": gekuerzt})
                    zugeordnet[abschnitt] = [t for t, _ in teile]
                    statistik["abschnitt_" + abschnitt] += 1
                    je_gruppe[gruppe]["abschnitt_" + abschnitt] += 1
            else:
                ohne_artikel.append(f"{p['deutsch']} ({p['lateinisch']})")
            if roh.get("label_de") and normalisiere(roh["label_de"]) not in (normalisiere(p["deutsch"]), normalisiere(p["lateinisch"])) \
                    and not sieht_lateinisch_aus(roh["label_de"], p["lateinisch"]):
                namensabweichung.append(f"{p['deutsch']} → Wikidata: {roh['label_de']}")
        if roh and roh.get("wikidata_id") or roh:
            quellen["wikidata"] = {"art": "Wikidata", "titel": "Wikidata (Bezeichnungen, Aliase)", "adresse": "https://www.wikidata.org",
                                   "version": None, "lizenz": "CC0 1.0", "abrufdatum": None, "aenderung": None}
        with open(ziel_datei, "w", encoding="utf-8") as f:
            json.dump(eintrag, f, ensure_ascii=False, indent=2)
            f.write("\n")
        geschrieben += 1
        stichprobe_quelle.append((p, eintrag, zugeordnet))

    if abrufdaten and "wikidata" in quellen:
        quellen["wikidata"]["abrufdatum"] = max(abrufdaten)
    with open(quellen_pfad, "w", encoding="utf-8") as f:
        json.dump(quellen, f, ensure_ascii=False, indent=2)
        f.write("\n")

    os.makedirs(roh_ordner, exist_ok=True)
    schreibe_bericht(roh_ordner, statistik, je_gruppe, mehrdeutig, nicht_zugeordnet, ohne_wikidata, ohne_artikel,
                     namensabweichung, fehlende_roh, geschrieben, uebersprungen)
    schreibe_stichprobe(roh_ordner, stichprobe_quelle)
    print(f"{geschrieben} Pflanzendateien geschrieben, {uebersprungen} bestehende übersprungen.")
    if fehlende_roh:
        print(f"ACHTUNG: Für {len(fehlende_roh)} Pflanzen fehlen Rohdaten (hole_daten.py zu Ende laufen lassen).")
    print("Bericht: rohdaten/bericht.md, Stichprobe: rohdaten/stichprobe.md")
    print("Danach: daten/version.txt um 1 erhöhen und python3 tools/baue_grunddaten.py ausführen.")


def schreibe_bericht(ordner, st, je_gruppe, mehrdeutig, nicht_zugeordnet, ohne_wikidata, ohne_artikel,
                     namensabweichung, fehlende_roh, geschrieben, uebersprungen):
    n = max(st["pflanzen"], 1)
    z = ["# Bericht zur Datenbeschaffung (4b)", "",
         f"Pflanzen verarbeitet: **{st['pflanzen']}** (übersprungen, weil schon echt vorhanden: {uebersprungen}); "
         f"ohne Rohdaten: {len(fehlende_roh)}", "",
         "## Gesamt", "", "| Merkmal | Anzahl | Anteil |", "|---|---|---|",
         f"| Wikidata-Treffer | {st['mit_wikidata']} | {100 * st['mit_wikidata'] // n} % |",
         f"| Wikipedia-Artikel | {st['mit_artikel']} | {100 * st['mit_artikel'] // n} % |"]
    for a in ABSCHNITTE:
        z.append(f"| Abschnitt {a} gefüllt | {st['abschnitt_' + a]} | {100 * st['abschnitt_' + a] // n} % |")
    z += ["", "## Je Hauptgruppe", "", "| Gruppe | Pflanzen | Wikidata | Artikel | " + " | ".join(ABSCHNITTE) + " |",
          "|---|---|---|---|" + "---|" * len(ABSCHNITTE)]
    for g, c in sorted(je_gruppe.items()):
        z.append(f"| {g} | {c['pflanzen']} | {c['wikidata']} | {c['artikel']} | " +
                 " | ".join(str(c["abschnitt_" + a]) for a in ABSCHNITTE) + " |")
    z += ["", "## Häufigste Überschriften der obersten Ebene, die zu keinem Abschnitt passten (Top 40)", "",
          "Hier sehen wir, ob wichtige Überschriften fehlen.", ""]
    z += [f"- {t} ({c}×)" for t, c in nicht_zugeordnet.most_common(40)]
    z += ["", "## Überschriften, die zu mehreren Abschnitten passten und deshalb weggelassen wurden", ""]
    z += [f"- {t} ({c}×)" for t, c in mehrdeutig.most_common(40)] or ["- keine"]
    z += ["", f"## Kein Wikidata-Treffer ({len(ohne_wikidata)})", ""] + [f"- {x}" for x in ohne_wikidata] if ohne_wikidata else \
        ["", "## Kein Wikidata-Treffer (0)", ""]
    z += ["", f"## Kein Wikipedia-Artikel ({len(ohne_artikel)})", ""] + [f"- {x}" for x in ohne_artikel]
    z += ["", f"## Abweichende Bezeichnung in Wikidata ({len(namensabweichung)})", "",
          "Unser Listenname bleibt Hauptname; die Wikidata-Bezeichnung wird als weiterer Name übernommen.", ""]
    z += [f"- {x}" for x in namensabweichung]
    if st["leer_nach_kuerzen"]:
        z += ["", f"Hinweis: {st['leer_nach_kuerzen']} Abschnitte blieben beim Kürzen leer (erster Absatz ohne sicheres Satzende)."]
    with open(os.path.join(ordner, "bericht.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(z) + "\n")


def schreibe_stichprobe(ordner, liste):
    rnd = random.Random(42)
    mit_artikel = [x for x in liste if x[1]["wikipedia"]]
    basis = mit_artikel if len(mit_artikel) >= 30 else liste
    auswahl = rnd.sample(basis, min(30, len(basis)))
    z = ["# Stichprobe zum Gegenlesen (30 Pflanzen, immer dieselbe Auswahl)", "",
         "Je Pflanze: aus welcher Wikipedia-Überschrift welcher Abschnitt entstand und der Textanfang. "
         "Bitte prüfen: Passt der Text zum Abschnitt?", ""]
    for p, e, zugeordnet in auswahl:
        z += [f"## {p['deutsch']} ({p['lateinisch']})", "", f"Wikipedia: {e['wikipedia'] or 'kein Artikel'}", ""]
        if not e["angaben"]:
            z += ["Keine Abschnitte übernommen.", ""]
        for a in e["angaben"]:
            kopf = ", ".join(zugeordnet.get(a["abschnitt"], []))
            text = a["text"].replace("\n", " ")
            z.append(f"- **{a['abschnitt']}** (aus: {kopf}; {'Auszug' if a['auszug'] else 'ganzer Abschnitt'}, {len(a['text'])} Zeichen): "
                     f"{text[:220]}{'…' if len(text) > 220 else ''}")
        z.append("")
    with open(os.path.join(ordner, "stichprobe.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(z) + "\n")


if __name__ == "__main__":
    main()
