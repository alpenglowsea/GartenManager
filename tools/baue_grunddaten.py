#!/usr/bin/env python3
"""Baut aus den Textdateien in daten/ die Grunddaten-Datei der App.

Aufruf (im Hauptordner des Projekts):   python3 tools/baue_grunddaten.py

Eingabe:  daten/pflanzen/*.json, daten/quellen.json, daten/gruppen.json,
          daten/version.txt, daten/nummern.csv
Ausgabe:  app/src/main/assets/grunddaten.db
          app/src/main/assets/grunddaten.version   (Inhalt: <version>:<pruefsumme>)

Neue Pflanzen bekommen automatisch die naechste freie Nummer; sie wird in
daten/nummern.csv eingetragen und nie wieder vergeben. Nur Python-Standardbibliothek.
"""
import csv, hashlib, json, os, re, sqlite3, sys, unicodedata

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pflanzenliste import slug  # noqa: E402

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATEN = os.path.join(WURZEL, "daten")
ASSETS = os.path.join(WURZEL, "app", "src", "main", "assets")
DB = os.path.join(ASSETS, "grunddaten.db")
ABSCHNITTE = ["Herkunft", "Standort", "Wuchs und Laub", "Blüte", "Verwendung", "Pflege"]
NAMENSARTEN = ["haupt", "umgangssprachlich", "lateinisch"]


def normalisiere(s):
    """MUSS exakt wie Grunddaten.normalisiere in der App arbeiten."""
    s = s.lower()
    s = s.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if not unicodedata.combining(c))
    s = s.replace("-", " ")
    return re.sub(r"\s+", " ", s).strip()


def fehler(msg):
    print("FEHLER:", msg)
    sys.exit(1)


def lade_json(pfad):
    with open(pfad, encoding="utf-8") as f:
        return json.load(f)


def main():
    gruppen = lade_json(os.path.join(DATEN, "gruppen.json"))
    quellen = lade_json(os.path.join(DATEN, "quellen.json"))
    version = int(open(os.path.join(DATEN, "version.txt")).read().strip())
    testdaten = any(k == "test" for k in quellen)

    # Nummern lesen
    nr_pfad = os.path.join(DATEN, "nummern.csv")
    nummern = {}
    with open(nr_pfad, encoding="utf-8", newline="") as f:
        for z in csv.DictReader(f):
            if z["schluessel"] in nummern.values() or int(z["nummer"]) in nummern:
                fehler("Doppelter Eintrag in nummern.csv: " + str(z))
            nummern[int(z["nummer"])] = z["schluessel"]
    schl_zu_nr = {v: k for k, v in nummern.items()}

    dateien = sorted(fn for fn in os.listdir(os.path.join(DATEN, "pflanzen")) if fn.endswith(".json"))
    neu = []
    for fn in dateien:
        schl = fn[:-5]
        if schl not in schl_zu_nr:
            nxt = max(nummern) + 1 if nummern else 1
            nummern[nxt] = schl
            schl_zu_nr[schl] = nxt
            neu.append((nxt, schl))
    if neu:
        with open(nr_pfad, "a", encoding="utf-8", newline="") as f:
            for n, s in neu:
                f.write(f"{n},{s}\n")
        print(f"{len(neu)} neue Pflanzennummern vergeben.")

    os.makedirs(ASSETS, exist_ok=True)
    if os.path.exists(DB):
        os.remove(DB)
    db = sqlite3.connect(DB)
    db.execute("PRAGMA journal_mode=DELETE")
    db.executescript("""
    CREATE TABLE meta (schluessel TEXT PRIMARY KEY, wert TEXT NOT NULL);
    CREATE TABLE gruppe (id INTEGER PRIMARY KEY, name TEXT NOT NULL);
    CREATE TABLE pflanze (id INTEGER PRIMARY KEY, hauptname TEXT NOT NULL, lateinisch TEXT NOT NULL,
        gattung TEXT NOT NULL, wikidata TEXT, wikipedia TEXT, ersetzt_durch INTEGER,
        veraltet INTEGER NOT NULL DEFAULT 0, sortname TEXT NOT NULL);
    CREATE TABLE name (id INTEGER PRIMARY KEY, pflanze_id INTEGER NOT NULL, name TEXT NOT NULL,
        art TEXT NOT NULL, quelle TEXT, suchtext TEXT NOT NULL);
    CREATE INDEX idx_name_pflanze ON name(pflanze_id);
    CREATE TABLE pflanze_gruppe (pflanze_id INTEGER NOT NULL, gruppe_id INTEGER NOT NULL,
        haupt INTEGER NOT NULL, PRIMARY KEY (pflanze_id, gruppe_id));
    CREATE TABLE quelle (schluessel TEXT PRIMARY KEY, art TEXT NOT NULL, titel TEXT NOT NULL,
        adresse TEXT, version TEXT, lizenz TEXT, abrufdatum TEXT, aenderung TEXT);
    CREATE TABLE angabe (id INTEGER PRIMARY KEY, pflanze_id INTEGER NOT NULL, abschnitt TEXT NOT NULL,
        text TEXT NOT NULL, quelle TEXT NOT NULL, auszug INTEGER NOT NULL DEFAULT 0);
    CREATE INDEX idx_angabe_pflanze ON angabe(pflanze_id);
    CREATE TABLE pflegeart (id INTEGER PRIMARY KEY, name TEXT NOT NULL, farbe TEXT);
    CREATE TABLE pflegehinweis (id INTEGER PRIMARY KEY, pflanze_id INTEGER NOT NULL, pflegeart_id INTEGER NOT NULL,
        text TEXT NOT NULL, von_monat INTEGER NOT NULL, bis_monat INTEGER NOT NULL, quelle TEXT NOT NULL);
    CREATE TABLE merkmal (id INTEGER PRIMARY KEY, pflanze_id INTEGER NOT NULL, schluessel TEXT NOT NULL, wert TEXT NOT NULL);
    CREATE TABLE danke (id INTEGER PRIMARY KEY, text TEXT NOT NULL);
    CREATE TABLE bild (pflanze_id INTEGER PRIMARY KEY, datei TEXT NOT NULL, urheber TEXT, lizenz TEXT NOT NULL,
        lizenz_url TEXT, seite TEXT, titel TEXT, herkunft TEXT, abruf TEXT);
    """)

    for i, name in enumerate(gruppen, 1):
        db.execute("INSERT INTO gruppe VALUES (?,?)", (i, name))
    gruppe_id = {n: i for i, n in enumerate(gruppen, 1)}

    for k, q in quellen.items():
        db.execute("INSERT INTO quelle VALUES (?,?,?,?,?,?,?,?)",
                   (k, q["art"], q["titel"], q.get("adresse"), q.get("version"),
                    q.get("lizenz"), q.get("abrufdatum"), q.get("aenderung")))

    bilder = {}
    bilder_pfad = os.path.join(DATEN, "bilder.json")
    if os.path.exists(bilder_pfad):
        bilder = lade_json(bilder_pfad)
    bilder_benutzt = set()
    anzahl_namen = 0
    gesehen_namen = set()
    for fn in dateien:
        p = lade_json(os.path.join(DATEN, "pflanzen", fn))
        pid = schl_zu_nr[fn[:-5]]
        wo = fn
        for feld in ("hauptname", "lateinisch", "gruppen"):
            if not p.get(feld):
                fehler(f"{wo}: Feld '{feld}' fehlt oder ist leer")
        if (p["hauptname"], p["lateinisch"]) in gesehen_namen:
            fehler(f"{wo}: Hauptname + lateinischer Name doppelt")
        gesehen_namen.add((p["hauptname"], p["lateinisch"]))
        gattung = p["lateinisch"].split()[0]
        db.execute("INSERT INTO pflanze VALUES (?,?,?,?,?,?,?,?,?)",
                   (pid, p["hauptname"], p["lateinisch"], gattung, p.get("wikidata"), p.get("wikipedia"),
                    None, 1 if p.get("veraltet") else 0, normalisiere(p["hauptname"])))
        b = bilder.get(slug(p["lateinisch"]))
        if b:
            if not os.path.exists(os.path.join(ASSETS, "bilder", b["datei"])):
                fehler(f"{wo}: Bilddatei fehlt in assets/bilder: {b['datei']} (baue_bilder.py laufen lassen)")
            db.execute("INSERT INTO bild VALUES (?,?,?,?,?,?,?,?,?)",
                       (pid, b["datei"], b.get("urheber"), b["lizenz"], b.get("lizenz_url"), b.get("seite"),
                        b.get("titel"), b.get("herkunft"), b.get("abruf")))
            bilder_benutzt.add(slug(p["lateinisch"]))
        namen = [(p["hauptname"], "haupt", None), (p["lateinisch"], "lateinisch", None)]
        for n in p.get("namen", []):
            if n["art"] not in NAMENSARTEN:
                fehler(f"{wo}: unbekannte Namensart {n['art']}")
            namen.append((n["name"], n["art"], n.get("quelle")))
        for n, art, qu in namen:
            if qu is not None and qu not in quellen:
                fehler(f"{wo}: unbekannte Quelle {qu}")
            db.execute("INSERT INTO name (pflanze_id,name,art,quelle,suchtext) VALUES (?,?,?,?,?)",
                       (pid, n, art, qu, normalisiere(n)))
            anzahl_namen += 1
        for j, g in enumerate(p["gruppen"]):
            if g not in gruppe_id:
                fehler(f"{wo}: unbekannte Gruppe {g}")
            db.execute("INSERT INTO pflanze_gruppe VALUES (?,?,?)", (pid, gruppe_id[g], 1 if j == 0 else 0))
        for a in p.get("angaben", []):
            if a["abschnitt"] not in ABSCHNITTE:
                fehler(f"{wo}: unbekannter Abschnitt {a['abschnitt']}")
            if a["quelle"] not in quellen:
                fehler(f"{wo}: unbekannte Quelle {a['quelle']}")
            db.execute("INSERT INTO angabe (pflanze_id,abschnitt,text,quelle,auszug) VALUES (?,?,?,?,?)",
                       (pid, a["abschnitt"], a["text"], a["quelle"], 1 if a.get("auszug") else 0))
        for h in p.get("pflegehinweise", []):
            if not (1 <= h["von_monat"] <= 12 and 1 <= h["bis_monat"] <= 12):
                fehler(f"{wo}: Monat ausserhalb 1-12")

    unbenutzt = sorted(set(bilder) - bilder_benutzt)
    if unbenutzt:
        print(f"Hinweis: {len(unbenutzt)} Bilder in daten/bilder.json gehören zu keiner Pflanze, zum Beispiel: {unbenutzt[:3]}")
    db.execute("INSERT INTO meta VALUES ('version', ?)", (str(version),))
    db.execute("INSERT INTO meta VALUES ('testdaten', ?)", ("1" if testdaten else "0",))
    db.execute("PRAGMA user_version=%d" % version)
    db.commit()
    db.execute("VACUUM")
    db.close()

    sha = hashlib.sha256(open(DB, "rb").read()).hexdigest()[:8]
    with open(os.path.join(ASSETS, "grunddaten.version"), "w") as f:
        f.write(f"{version}:{sha}")
    print(f"Fertig: {len(dateien)} Pflanzen, {anzahl_namen} Namen, {len(bilder_benutzt)} Bilder, Version {version}:{sha}")
    print("Datei:", DB)


if __name__ == "__main__":
    main()
