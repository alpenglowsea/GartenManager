#!/usr/bin/env python3
"""Holt für die Pflanzen ein freies Bild von Wikimedia Commons, samt Urheber und Lizenz.

Aufruf (im Hauptordner des Projekts):
    python3 tools/hole_bilder.py --test 20      # Testlauf: 20 gleichmäßig verteilte Pflanzen
    python3 tools/hole_bilder.py                # alle Pflanzen (fortsetzbar)
    python3 tools/hole_bilder.py --ersatz       # für Bilder aus daten/bilder_ausschluss.txt das nächste Bild suchen
    python3 tools/hole_bilder.py --maxlag 60    # Server darf stärker verzögert sein, bevor wir warten (Standard 30)
    python3 tools/hole_bilder.py --berichte     # nur Bericht und Kontaktbögen neu schreiben (kein Abruf)

Voraussetzung: hole_daten.py ist gelaufen (wir brauchen die Wikidata-Nummer in rohdaten/).

Quellen für ein Bild, in dieser Reihenfolge:
  1. das Bild des Wikidata-Eintrags (Eigenschaft P18), "bevorzugte" zuerst
  2. das Titelbild des deutschen Wikipedia-Artikels
Es wird nur ein Bild genommen, dessen Lizenz frei ist: CC0, gemeinfrei, CC BY, CC BY-SA.
Alles mit "nicht kommerziell", "keine Bearbeitung" oder unklarer Lizenz wird übersprungen.
Heruntergeladen wird eine verkleinerte Fassung (960 Pixel breit), nicht das Original.

Ergebnis (nur im Ordner rohdaten/, sonst wird nichts verändert):
    rohdaten/bilder/<name>.jpg        die Bilder
    rohdaten/bilder.json              Urheber, Lizenz, Quelle je Bild
    rohdaten/bilder_bericht.md        Zahlen und Gründe für fehlende Bilder
    rohdaten/kontaktbogen_NN.html     Übersichtsseiten zum Durchsehen im Browser (mit Ausschlussliste)

Nur Python-Standardbibliothek; Pillow (Paket python3-pil) wird nur für die Größenschätzung benutzt.
"""
import argparse
import html
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hole_daten import Netzfehler, PAUSE, USER_AGENT, abfragen  # noqa: E402
from pflanzenliste import WURZEL, finde_liste, lies_liste, slug  # noqa: E402

BREITE = 960          # Breite der heruntergeladenen Fassung (ein Standardmaß von Wikimedia)
MIN_BREITE = 500      # kleinere Originale sind für die Kopfzeile des Info-Fensters zu grob
ERLAUBTE_TYPEN = {"image/jpeg", "image/png", "image/webp"}
SCHAETZ_KANTE = 480   # nur für die Größenschätzung: so groß könnte das Bild in der App sein
BOGEN_GROESSE = 60


# ---------- Hilfen ----------

API_MAXLAG = 30  # Sekunden; wird mit --maxlag geändert (0 = Angabe weglassen)


def api(url):
    """Ruft eine API-Adresse ab; der Wert für maxlag kommt aus API_MAXLAG statt aus der Adresse."""
    url = re.sub(r"&?maxlag=\d+", "", url)
    if API_MAXLAG:
        url += "&maxlag=%d" % API_MAXLAG
    return abfragen(url)


def wikidata_claims(eid):
    url = "https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
        "action": "wbgetentities", "ids": eid, "props": "claims", "format": "json"})
    return api(url).get("entities", {}).get(eid, {})

def lade_datei(url, ziel):
    """Lädt eine Datei herunter (bis zu 4 Versuche). Wirft Netzfehler."""
    letzter = None
    for versuch in range(1, 5):
        warte = 5 * versuch
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=60) as antwort:
                daten = antwort.read()
            with open(ziel + ".tmp", "wb") as f:
                f.write(daten)
            os.replace(ziel + ".tmp", ziel)
            time.sleep(PAUSE)
            return len(daten)
        except urllib.error.HTTPError as e:
            letzter = f"HTTP {e.code}"
            if e.code in (429, 503):
                warte = max(int(e.headers.get("Retry-After", "0") or 0), 30 * versuch)
        except Exception as e:
            letzter = f"{type(e).__name__}: {e}"
        if versuch < 4:
            print(f"      ({letzter}; Versuch {versuch}, warte {warte} s)", flush=True)
            time.sleep(warte)
    raise Netzfehler(letzter)


def datei_titel(name):
    """'Foo_bar.jpg' oder 'File:Foo bar.jpg' -> 'File:Foo bar.jpg'."""
    n = name.strip()
    if n.lower().startswith("file:") or n.lower().startswith("datei:"):
        n = n.split(":", 1)[1]
    n = n.replace("_", " ").strip()
    return "File:" + (n[:1].upper() + n[1:])


def text_aus_html(s):
    s = re.sub(r"<[^>]+>", "", s or "")
    return re.sub(r"\s+", " ", html.unescape(s)).strip()


def pruefe_lizenz(meta):
    """Gibt (ok, kurzname, grund) zurück."""
    kurz = (meta.get("LicenseShortName", {}).get("value") or "").strip()
    l = kurz.lower()
    if not kurz:
        return False, "", "keine Lizenzangabe"
    if re.search(r"\b(nc|nd)\b|non-?commercial|no[- ]?deriv", l):
        return False, kurz, "nicht kommerziell / keine Bearbeitung: " + kurz
    if re.match(r"(cc0|cc zero|cc[- ]pdm|public domain|pd\b|pd-)", l):
        return True, kurz, ""
    if re.match(r"cc[ -]by(-sa)?[ -]\d\.\d", l):
        return True, kurz, ""
    return False, kurz, "Lizenz nicht auf unserer Liste: " + kurz


def commons_info(titel_liste):
    """Fragt Bildinformationen für mehrere Dateien ab. Gibt {titel: info} zurück (info=None, wenn es die Datei nicht gibt)."""
    if not titel_liste:
        return {}
    url = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
        "action": "query", "titles": "|".join(titel_liste), "prop": "imageinfo", "redirects": 1,
        "iiprop": "url|size|mime|extmetadata", "iiurlwidth": BREITE, "format": "json", "maxlag": 5,
    })
    antwort = api(url)
    q = antwort.get("query", {})
    umbenennung = {}
    for liste in (q.get("normalized", []), q.get("redirects", [])):
        for u in liste:
            umbenennung[u["from"]] = u["to"]

    def endgueltig(t):
        for _ in range(3):
            t = umbenennung.get(t, t)
        return t

    seiten = {s.get("title"): s for s in q.get("pages", {}).values()}
    ergebnis = {}
    for t in titel_liste:
        s = seiten.get(endgueltig(t))
        ii = (s or {}).get("imageinfo") or []
        ergebnis[t] = ii[0] if ii and "missing" not in s else None
        if ergebnis[t] is not None:
            ergebnis[t]["_titel"] = s.get("title")
    return ergebnis


def kandidaten_fuer(roh):
    """Liste von (Dateititel, Herkunft) für eine Pflanze, beste zuerst."""
    liste = []
    eid = roh.get("wikidata_id")
    if eid:
        e = wikidata_claims(eid)
        bevorzugt, normal = [], []
        for a in e.get("claims", {}).get("P18", []):
            w = a.get("mainsnak", {}).get("datavalue", {}).get("value")
            if not isinstance(w, str) or a.get("rank") == "deprecated":
                continue
            (bevorzugt if a.get("rank") == "preferred" else normal).append(w)
        for w in bevorzugt + normal:
            liste.append((datei_titel(w), "Wikidata P18"))
    dewiki = roh.get("dewiki_titel") or (roh.get("wikipedia") or {}).get("titel")
    if dewiki:
        url = "https://de.wikipedia.org/w/api.php?" + urllib.parse.urlencode({
            "action": "query", "prop": "pageimages", "piprop": "name", "titles": dewiki,
            "redirects": 1, "format": "json", "maxlag": 5,
        })
        for seite in api(url).get("query", {}).get("pages", {}).values():
            if seite.get("pageimage"):
                liste.append((datei_titel(seite["pageimage"]), "Titelbild des Wikipedia-Artikels"))
    gesehen, eindeutig = set(), []
    for t, h in liste:
        if t not in gesehen:
            gesehen.add(t)
            eindeutig.append((t, h))
    return eindeutig


def waehle(kandidaten, ab, ordner, name):
    """Probiert die Kandidaten ab Position [ab] und lädt das erste mit freier Lizenz herunter.
    Gibt (eintrag, ablehnungen) zurück; eintrag ist None, wenn keiner passt."""
    ablehnungen = []
    infos = commons_info([t for t, _ in kandidaten[ab:]])
    for i in range(ab, len(kandidaten)):
        titel, herkunft = kandidaten[i]
        info = infos.get(titel)
        if not info:
            ablehnungen.append(f"{titel}: Datei auf Commons nicht gefunden")
            continue
        meta = info.get("extmetadata", {})
        ok, kurz, grund = pruefe_lizenz(meta)
        urheber = text_aus_html(meta.get("Artist", {}).get("value")) or text_aus_html(meta.get("Attribution", {}).get("value"))
        if not urheber:
            urheber = ""
        if ok and not urheber and kurz.lower().startswith("cc by"):
            ok, grund = False, "Urheber fehlt (bei CC BY und CC BY-SA Pflicht): " + kurz
        if ok and info.get("mime") not in ERLAUBTE_TYPEN:
            ok, grund = False, "Dateityp " + str(info.get("mime"))
        if ok and (info.get("width") or 0) < MIN_BREITE:
            ok, grund = False, f"zu klein ({info.get('width')} Pixel breit)"
        if not ok:
            ablehnungen.append(f"{titel}: {grund}")
            continue
        endung = os.path.splitext(urllib.parse.urlparse(info["thumburl"]).path)[1].lower() or ".jpg"
        if endung not in (".jpg", ".jpeg", ".png", ".webp"):
            endung = ".jpg"
        datei = name + endung
        for alt in os.listdir(ordner):
            if os.path.splitext(alt)[0] == name:
                os.remove(os.path.join(ordner, alt))
        groesse = lade_datei(info["thumburl"], os.path.join(ordner, datei))
        return {
            "datei": datei, "bytes": groesse, "commons_titel": info["_titel"], "herkunft": herkunft,
            "seite": info.get("descriptionurl") or "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(info["_titel"].replace(" ", "_")),
            "urheber": urheber, "lizenz": kurz,
            "lizenz_url": (meta.get("LicenseUrl", {}).get("value") or ""),
            "breite_original": info.get("width"), "hoehe_original": info.get("height"),
            "index": i, "abruf": time.strftime("%Y-%m-%d"),
        }, ablehnungen
    return None, ablehnungen


# ---------- Berichte ----------

def schaetze_endgroesse(pfad):
    """Wie groß wäre das Bild als WebP in der App? Nur eine Schätzung; braucht Pillow."""
    try:
        import io
        from PIL import Image
        with Image.open(pfad) as im:
            im = im.convert("RGB")
            im.thumbnail((SCHAETZ_KANTE, SCHAETZ_KANTE))
            puffer = io.BytesIO()
            im.save(puffer, "WEBP", quality=75)
            return len(puffer.getvalue())
    except Exception:
        return None


def vorschau_adresse(ordner, datei):
    """Quadratischer Ausschnitt (Mitte) als eingebettetes Bild, damit die Kontaktbögen ohne Dateipfade funktionieren.
    Ohne Pillow: normaler Dateiverweis."""
    try:
        import base64
        import io
        from PIL import Image, ImageOps
        with Image.open(os.path.join(ordner, datei)) as im:
            im = ImageOps.fit(im.convert("RGB"), (300, 300))
            puffer = io.BytesIO()
            im.save(puffer, "JPEG", quality=80)
        return "data:image/jpeg;base64," + base64.b64encode(puffer.getvalue()).decode("ascii")
    except Exception:
        return "bilder/" + datei


def schreibe_berichte(ordner, bilder, namen):
    mit = {k: v for k, v in bilder.items() if v.get("datei")}
    ohne = {k: v for k, v in bilder.items() if not v.get("datei")}
    zeilen = ["# Bericht Bilder", "", f"Stand: {time.strftime('%d.%m.%Y %H:%M')}", ""]
    zeilen.append(f"- Pflanzen bearbeitet (nach lateinischem Namen): **{len(bilder)}**")
    zeilen.append(f"- mit freiem Bild: **{len(mit)}** ({100 * len(mit) // max(1, len(bilder))} %)")
    zeilen.append(f"- ohne Bild: **{len(ohne)}**")
    zeilen.append("")
    if mit:
        gr = [v["bytes"] for v in mit.values()]
        zeilen.append(f"- Größe der heruntergeladenen Fassung ({BREITE} px): Durchschnitt {sum(gr) // len(gr) // 1024} KB, "
                      f"größtes {max(gr) // 1024} KB")
        schaetz = []
        for v in mit.values():
            s = schaetze_endgroesse(os.path.join(ordner, v["datei"]))
            if s:
                schaetz.append(s)
        if schaetz:
            mittel = sum(schaetz) // len(schaetz)
            zeilen.append(f"- **Schätzung** der Größe in der App (WebP, längste Kante {SCHAETZ_KANTE} px, Qualität 75): "
                          f"Durchschnitt {mittel // 1024} KB je Bild, bei 539 Pflanzen etwa {mittel * 539 // 1024 // 1024} MB")
        else:
            zeilen.append("- Größenschätzung übersprungen (Pillow fehlt: `sudo apt install python3-pil`)")
    zeilen += ["", "## Lizenzen der gefundenen Bilder", ""]
    zaehl = {}
    for v in mit.values():
        zaehl[v["lizenz"]] = zaehl.get(v["lizenz"], 0) + 1
    for l, n in sorted(zaehl.items(), key=lambda x: -x[1]):
        zeilen.append(f"- {l}: {n}")
    zeilen += ["", "## Herkunft der gefundenen Bilder", ""]
    zaehl = {}
    for v in mit.values():
        zaehl[v["herkunft"]] = zaehl.get(v["herkunft"], 0) + 1
    for l, n in sorted(zaehl.items(), key=lambda x: -x[1]):
        zeilen.append(f"- {l}: {n}")
    zeilen += ["", "## Pflanzen ohne Bild und warum", ""]
    for k, v in sorted(ohne.items()):
        zeilen.append(f"- **{namen.get(k, k)}** ({k}): {v.get('grund', '?')}")
        for a in v.get("ablehnungen", [])[:4]:
            zeilen.append(f"    - {a}")
    with open(os.path.join(os.path.dirname(ordner), "bilder_bericht.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(zeilen) + "\n")

    # Kontaktbögen
    namensliste = sorted(mit, key=lambda k: namen.get(k, k).lower())
    seiten = [namensliste[i:i + BOGEN_GROESSE] for i in range(0, len(namensliste), BOGEN_GROESSE)]
    wurzel = os.path.dirname(ordner)
    for alt in os.listdir(wurzel):
        if alt.startswith("kontaktbogen_") and alt.endswith(".html"):
            os.remove(os.path.join(wurzel, alt))
    for nr, seite in enumerate(seiten, 1):
        zellen = []
        for k in seite:
            v = mit[k]
            zellen.append(
                f'<label class="z"><input type="checkbox" value="{html.escape(k)}">'
                f'<a href="bilder/{html.escape(v["datei"])}" target="_blank"><img src="{vorschau_adresse(ordner, v["datei"])}"></a>'
                f'<b>{html.escape(namen.get(k, k))}</b><i>{html.escape(k.replace("-", " "))}</i>'
                f'<span>{html.escape(v["lizenz"])} · {html.escape(v["herkunft"].replace("Titelbild des Wikipedia-Artikels", "Wikipedia"))}</span></label>')
        navi = " ".join(f'<a href="kontaktbogen_{i:02d}.html">{i}</a>' for i in range(1, len(seiten) + 1))
        with open(os.path.join(wurzel, f"kontaktbogen_{nr:02d}.html"), "w", encoding="utf-8") as f:
            f.write(KOPF.replace("%TITEL%", f"Kontaktbogen {nr} von {len(seiten)}").replace("%NAVI%", navi)
                    + "\n".join(zellen) + FUSS)
    return len(mit), len(ohne), len(seiten)


KOPF = """<!doctype html><html lang="de"><head><meta charset="utf-8"><title>%TITEL%</title><style>
body{font-family:sans-serif;margin:16px;background:#fafafa} h1{font-size:18px} nav a{margin-right:8px}
.raster{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:10px}
.z{display:block;background:#fff;border:2px solid #ddd;border-radius:8px;padding:6px;font-size:12px;position:relative}
.z img{width:100%;aspect-ratio:1/1;object-fit:cover;border-radius:4px;display:block}
.z b,.z i,.z span{display:block;margin-top:3px}.z i{color:#555}.z span{color:#888;font-size:11px}
.z input{position:absolute;top:10px;left:10px;width:22px;height:22px}
.z:has(input:checked){border-color:#c0392b;background:#fdecea}
textarea{width:100%;height:110px;margin-top:12px}
</style></head><body><h1>%TITEL%</h1>
<p>Die Bilder sind so zugeschnitten, wie sie auf einer Kachel erscheinen würden (quadratisch, Mitte). Ein Klick auf das Bild zeigt es ganz.
Setze ein Häkchen bei jedem <b>ungeeigneten</b> Bild (falsche Pflanze, nur Frucht oder Herbarbeleg, ganzer Garten, Personen, unscharf).</p>
<nav>Seiten: %NAVI%</nav><div class="raster">
"""
FUSS = """</div><p>Ausschlussliste dieser Seite (zum Kopieren in <code>daten/bilder_ausschluss.txt</code>, ein Name pro Zeile):</p>
<textarea id="liste" readonly></textarea>
<script>
const boxen=[...document.querySelectorAll('input[type=checkbox]')],t=document.getElementById('liste');
function neu(){t.value=boxen.filter(b=>b.checked).map(b=>b.value).join('\\n')}
boxen.forEach(b=>b.addEventListener('change',neu));neu();
</script></body></html>
"""


# ---------- Hauptprogramm ----------

def lade_bilder_json(pfad):
    if os.path.exists(pfad):
        with open(pfad, encoding="utf-8") as f:
            return json.load(f)
    return {}


def speichere(pfad, daten):
    with open(pfad + ".tmp", "w", encoding="utf-8") as f:
        json.dump(daten, f, ensure_ascii=False, indent=1)
    os.replace(pfad + ".tmp", pfad)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--test", type=int, default=0, help="nur N gleichmäßig verteilte Pflanzen")
    ap.add_argument("--ersatz", action="store_true", help="Ersatz für Bilder aus daten/bilder_ausschluss.txt suchen")
    ap.add_argument("--berichte", action="store_true", help="nur Bericht und Kontaktbögen neu schreiben")
    ap.add_argument("--maxlag", type=int, default=30,
                    help="Wie stark der Wikimedia-Server verzögert sein darf, bevor wir warten (Sekunden, Standard 30; 0 = nie warten)")
    ap.add_argument("--wurzel", default=WURZEL)
    args = ap.parse_args()
    global API_MAXLAG
    API_MAXLAG = args.maxlag

    pflanzen = lies_liste(finde_liste(args.wurzel))
    roh_ordner = os.path.join(args.wurzel, "rohdaten")
    ordner = os.path.join(roh_ordner, "bilder")
    os.makedirs(ordner, exist_ok=True)
    json_pfad = os.path.join(roh_ordner, "bilder.json")
    bilder = lade_bilder_json(json_pfad)

    je_latein = {}
    for p in pflanzen:
        je_latein.setdefault(slug(p["lateinisch"]), p)
    namen = {k: p["deutsch"] for k, p in je_latein.items()}

    if args.berichte:
        m, o, s = schreibe_berichte(ordner, bilder, namen)
        print(f"Bericht und {s} Kontaktbögen geschrieben ({m} mit Bild, {o} ohne).")
        return

    if args.ersatz:
        pfad = os.path.join(args.wurzel, "daten", "bilder_ausschluss.txt")
        if not os.path.exists(pfad):
            print("Keine Datei daten/bilder_ausschluss.txt gefunden.")
            return
        arbeit = [z.strip() for z in open(pfad, encoding="utf-8") if z.strip() and not z.startswith("#")]
    else:
        arbeit = list(je_latein)
        if args.test:
            schritt = max(1, len(arbeit) // args.test)
            arbeit = arbeit[::schritt][: args.test]

    start = time.time()
    in_folge = 0
    fehlgeschlagen = []
    for nr, k in enumerate(arbeit, 1):
        if k not in je_latein:
            print(f"Unbekannter Name in der Liste: {k}")
            continue
        if not args.ersatz and k in bilder:
            continue
        p = je_latein[k]
        roh_pfad = os.path.join(roh_ordner, k + ".json")
        print(f"[{nr}/{len(arbeit)}] {p['deutsch']} ({p['lateinisch']}) ...", flush=True)
        if not os.path.exists(roh_pfad):
            bilder[k] = {"grund": "keine Rohdaten (hole_daten.py noch nicht gelaufen)"}
            speichere(json_pfad, bilder)
            continue
        try:
            if args.ersatz:
                alt = bilder.get(k, {})
                kandidaten = alt.get("kandidaten")
                ab = alt.get("index", -1) + 1
                if not kandidaten:
                    print("      Kein früherer Abruf gespeichert, übersprungen.")
                    continue
            else:
                roh = json.load(open(roh_pfad, encoding="utf-8"))
                kandidaten = kandidaten_fuer(roh)
                ab = 0
            if not kandidaten:
                bilder[k] = {"grund": "weder Wikidata-Bild noch Wikipedia-Titelbild vorhanden", "kandidaten": []}
            else:
                eintrag, abl = waehle(kandidaten, ab, ordner, k)
                if eintrag:
                    eintrag["kandidaten"] = kandidaten
                    bilder[k] = eintrag
                    print(f"      {eintrag['lizenz']}, {eintrag['bytes'] // 1024} KB, {eintrag['herkunft']}", flush=True)
                else:
                    for alt_datei in os.listdir(ordner):
                        if os.path.splitext(alt_datei)[0] == k:
                            os.remove(os.path.join(ordner, alt_datei))
                    bilder[k] = {"grund": "kein Kandidat mit freier Lizenz" if ab == 0 else "kein weiterer Kandidat",
                                 "ablehnungen": abl, "kandidaten": kandidaten, "index": len(kandidaten)}
                    print("      kein passendes Bild:", abl[:1], flush=True)
        except Netzfehler as fehler:
            print("      NETZFEHLER, wird beim nächsten Start erneut versucht:", fehler, flush=True)
            fehlgeschlagen.append(k)
            in_folge += 1
            if in_folge >= 5:
                print("\nMehrere Fehler hintereinander. Abbruch; später erneut starten (fertige Bilder sind gespeichert).")
                break
            continue
        in_folge = 0
        speichere(json_pfad, bilder)

    print(f"\nFertig nach {(time.time() - start) / 60:.1f} Minuten. Netzfehler bei {len(fehlgeschlagen)} Pflanzen.")
    if args.ersatz and not fehlgeschlagen:
        # Liste ist abgearbeitet; umbenennen, damit dieselben Bilder nicht beim nächsten Mal noch einmal getauscht werden
        erledigt = pfad + time.strftime(".erledigt-%Y%m%d-%H%M%S")
        os.replace(pfad, erledigt)
        print("Die Ausschlussliste wurde abgearbeitet und umbenannt in", os.path.basename(erledigt))
    m, o, s = schreibe_berichte(ordner, bilder, namen)
    print(f"{m} Pflanzen mit Bild, {o} ohne. Bericht: rohdaten/bilder_bericht.md, Kontaktbögen: rohdaten/kontaktbogen_01.html ...")
    if fehlgeschlagen:
        print("Bitte das Skript noch einmal starten, es holt nur die fehlenden nach.")


if __name__ == "__main__":
    main()
