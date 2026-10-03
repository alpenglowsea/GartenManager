#!/usr/bin/env python3
"""Holt für alle Pflanzen der Pflanzenliste Daten von Wikidata und der deutschen Wikipedia.

Aufruf (im Hauptordner des Projekts):
    python3 tools/hole_daten.py              # alle Pflanzen (fortsetzbar)
    python3 tools/hole_daten.py --test 10    # nur die ersten 10 (zum Ausprobieren)
    python3 tools/hole_daten.py --luecken    # Pflanzen ohne Treffer/Artikel mit den neueren Suchwegen nochmal

Das Skript liest nur aus dem Internet und schreibt ausschließlich in den Ordner rohdaten/
(eine Datei je lateinischem Namen, unverändert wie geliefert). Es ändert nichts anderes.
Wird es unterbrochen, einfach erneut starten: fertige Pflanzen werden übersprungen.
Nur Python-Standardbibliothek. Zwischen zwei Abfragen wird gewartet (Höflichkeit gegenüber Wikimedia).
"""
import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pflanzenliste import WURZEL, finde_liste, lies_liste, slug  # noqa: E402

USER_AGENT = "GartenManager-Datenabruf/0.2 (https://github.com/alpenglowsea/GartenManager; freie Gartenplanungs-App)"
PAUSE = 1.0


class Netzfehler(Exception):
    pass


def abfragen(url):
    """Ruft eine Adresse ab. Bei Netzproblemen bis zu 4 Versuche, bei hoher Serverlast (maxlag)
    bis zu 12 Versuche mit längeren Pausen. Wirft Netzfehler, wenn es nicht klappt."""
    letzter = None
    versuch = 0
    while True:
        versuch += 1
        maxlag = False
        warte = 5 * versuch
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=40) as antwort:
                retry_nach = antwort.headers.get("Retry-After")
                daten = json.loads(antwort.read().decode("utf-8"))
            if "error" in daten and daten["error"].get("code") == "maxlag":
                maxlag = True
                letzter = "Server ausgelastet (maxlag)"
                warte = max(int(retry_nach or 0), 30 * min(versuch, 6))
            else:
                time.sleep(PAUSE)
                return daten
        except urllib.error.HTTPError as e:
            letzter = f"HTTP {e.code}"
            if e.code in (429, 503):
                warte = max(int(e.headers.get("Retry-After", "0") or 0), warte)
        except Exception as e:  # Netz weg, Zeitüberschreitung, kaputte Antwort
            letzter = f"{type(e).__name__}: {e}"
        if versuch >= (12 if maxlag else 4):
            raise Netzfehler(letzter)
        print(f"      ({letzter}; Versuch {versuch}, warte {warte} s)", flush=True)
        time.sleep(warte)


def norm_taxon(name):
    """Vereinheitlicht die Schreibweise von Hybridzeichen: 'Nepeta × faassenii' = 'Nepeta ×faassenii'."""
    return re.sub(r"\s*×\s*", " ×", name.strip())


def ohne_klammern(name):
    return re.sub(r"\s*\(.*?\)\s*", " ", name).strip()


def wikidata_suche(name, sprache="en"):
    url = "https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
        "action": "wbsearchentities", "search": name, "language": sprache,
        "type": "item", "limit": 7, "format": "json", "maxlag": 5,
    })
    return [t["id"] for t in abfragen(url).get("search", [])]


def wikidata_entitaeten(ids, props="labels|aliases|claims|sitelinks"):
    if not ids:
        return {}
    url = "https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
        "action": "wbgetentities", "ids": "|".join(ids), "props": props,
        "languages": "de|en", "sitefilter": "dewiki", "format": "json", "maxlag": 5,
    })
    return abfragen(url).get("entities", {})


def aussagen(entitaet, prop):
    werte = []
    for a in entitaet.get("claims", {}).get(prop, []):
        w = a.get("mainsnak", {}).get("datavalue", {}).get("value")
        if w is not None:
            werte.append(w)
    return werte


def wikipedia_text(titel):
    url = "https://de.wikipedia.org/w/api.php?" + urllib.parse.urlencode({
        "action": "query", "prop": "extracts|revisions", "explaintext": 1,
        "exsectionformat": "wiki", "rvprop": "ids", "redirects": 1,
        "titles": titel, "format": "json", "maxlag": 5,
    })
    for seite in abfragen(url).get("query", {}).get("pages", {}).values():
        revs = seite.get("revisions", [])
        return {"titel": seite.get("title"), "revision": revs[0].get("revid") if revs else None,
                "text": seite.get("extract", "")}
    return {"titel": titel, "revision": None, "text": ""}


def artikel_passt(text, latein):
    """Prüft grob, ob ein Wikipedia-Text zur gesuchten Art gehört (Gattung und Artzusatz kommen vor)."""
    t = text.lower()
    teile = [x for x in latein.replace("×", " ").split() if x]
    if not teile or teile[0].lower() not in t:
        return False
    return len(teile) < 2 or teile[1].lower() in t


def artikel_ueber_listenname(p):
    """Notlösung: Wikipedia-Artikel über den deutschen Listennamen suchen."""
    kandidaten = [ohne_klammern(p["deutsch"])]
    kandidaten += [k.strip() for k in re.findall(r"\((.*?)\)", p["deutsch"])]
    for titel in kandidaten:
        if not titel:
            continue
        wp = wikipedia_text(titel)
        if wp.get("text") and artikel_passt(wp["text"], p["lateinisch"]):
            return wp
    return None


def verarbeite(p):
    """Holt die Daten für eine Pflanze. Wirft Netzfehler bei Netzproblemen (dann wird nichts gespeichert)."""
    ergebnis = {"latein_liste": p["lateinisch"], "synonyme_liste": p["synonyme"],
                "abruf": time.strftime("%Y-%m-%d"), "hinweise": []}
    gesucht = {norm_taxon(x) for x in [p["lateinisch"]] + p["synonyme"]}
    suchen = [(p["lateinisch"], "en", "name")] + [(s, "en", f"synonym: {s}") for s in p["synonyme"]]
    suchen.append((ohne_klammern(p["deutsch"]), "de", "deutscher Listenname"))
    treffer = None
    for name, sprache, art in suchen:
        kandidaten = wikidata_suche(name, sprache)
        entitaeten = wikidata_entitaeten(kandidaten)
        for eid in kandidaten:
            e = entitaeten.get(eid, {})
            werte = {norm_taxon(w) for w in aussagen(e, "P225") if isinstance(w, str)}
            if werte & gesucht:
                treffer = (eid, e, art)
                break
        if treffer:
            break
    dewiki = None
    if treffer:
        eid, e, art = treffer
        ergebnis["wikidata_id"] = eid
        ergebnis["treffer_ueber"] = art
        ergebnis["label_de"] = e.get("labels", {}).get("de", {}).get("value")
        ergebnis["aliase_de"] = [a["value"] for a in e.get("aliases", {}).get("de", [])]
        ergebnis["trivialnamen_de"] = [w.get("text") for w in aussagen(e, "P1843")
                                       if isinstance(w, dict) and w.get("language") == "de" and w.get("text")]
        dewiki = e.get("sitelinks", {}).get("dewiki", {}).get("title")
        ergebnis["dewiki_titel"] = dewiki
    else:
        ergebnis["hinweise"].append("Kein Wikidata-Eintrag mit genau diesem lateinischen Namen (P225) gefunden")
    if dewiki:
        ergebnis["wikipedia"] = wikipedia_text(dewiki)
    else:
        wp = artikel_ueber_listenname(p)
        if wp:
            ergebnis["wikipedia"] = wp
            ergebnis["artikel_ueber"] = "deutscher Listenname"
        else:
            ergebnis["hinweise"].append("Kein Artikel in der deutschen Wikipedia gefunden")
    return ergebnis


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--test", type=int, default=0, help="nur die ersten N Pflanzen holen")
    ap.add_argument("--luecken", action="store_true",
                    help="Pflanzen mit Hinweisen (kein Treffer, kein Artikel) noch einmal versuchen")
    ap.add_argument("--wurzel", default=WURZEL)
    args = ap.parse_args()

    pflanzen = lies_liste(finde_liste(args.wurzel))
    ordner = os.path.join(args.wurzel, "rohdaten")
    os.makedirs(ordner, exist_ok=True)
    # Nach lateinischem Namen zusammenfassen (Weißkohl und Rotkohl teilen sich eine Abfrage)
    je_latein = {}
    for p in pflanzen:
        je_latein.setdefault(p["lateinisch"], p)
    arbeit = list(je_latein.values())
    if args.test:
        arbeit = arbeit[: args.test]

    fehlgeschlagen = []
    in_folge = 0
    start = time.time()
    for nr, p in enumerate(arbeit, 1):
        datei = os.path.join(ordner, slug(p["lateinisch"]) + ".json")
        if os.path.exists(datei):
            if not args.luecken:
                continue
            if not json.load(open(datei, encoding="utf-8")).get("hinweise"):
                continue
        print(f"[{nr}/{len(arbeit)}] {p['deutsch']} ({p['lateinisch']}) ...", flush=True)
        try:
            erg = verarbeite(p)
        except Netzfehler as fehler:
            print("      NETZFEHLER, wird beim nächsten Start erneut versucht:", fehler, flush=True)
            fehlgeschlagen.append(p["lateinisch"])
            in_folge += 1
            if in_folge >= 5:
                print("\nMehrere Fehler hintereinander: Der Server ist wohl gerade überlastet. "
                      "Abbruch. Fertige Pflanzen sind gespeichert; bitte später (zum Beispiel in einer Stunde) "
                      "erneut starten.")
                break
            continue
        in_folge = 0
        if erg["hinweise"]:
            print("      Hinweis:", "; ".join(erg["hinweise"]), flush=True)
        with open(datei + ".tmp", "w", encoding="utf-8") as f:
            json.dump(erg, f, ensure_ascii=False, indent=1)
        os.replace(datei + ".tmp", datei)
    minuten = (time.time() - start) / 60
    print(f"\nFertig nach {minuten:.1f} Minuten. Netzfehler bei {len(fehlgeschlagen)} Pflanzen.")
    if fehlgeschlagen:
        print("Bitte das Skript noch einmal starten, es holt nur die fehlenden nach.")


if __name__ == "__main__":
    main()
