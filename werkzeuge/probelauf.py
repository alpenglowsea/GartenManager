"""
GartenManager - Probelauf (Version 0.1)

Was das Skript tut:
  Es fragt fuer 20 Pflanzen zwei oeffentliche Wikimedia-Dienste ab
  (Wikidata und die deutsche Wikipedia) und speichert alles unveraendert in
  der Datei probe_ergebnis.json. Es schreibt NICHTS ins Internet und
  veraendert keine anderen Dateien.

Es braucht nur Python, keine zusaetzlichen Pakete.
Dauer: etwa 2 Minuten (zwischen den Abfragen wird bewusst gewartet).
"""
import json
import sys
import time
import urllib.parse
import urllib.request

USER_AGENT = "GartenManager-Probelauf/0.1 (https://github.com/alpenglowsea/GartenManager)"
PAUSE = 1.0  # Sekunden zwischen zwei Abfragen (Hoeflichkeit gegenueber Wikimedia)

PFLANZEN = [
    ("Echter Lavendel", "Lavandula angustifolia"),
    ("Rosmarin", "Salvia rosmarinus"),
    ("Tomate", "Solanum lycopersicum"),
    ("Apfel", "Malus domestica"),
    ("Hunds-Rose", "Rosa canina"),
    ("Gartenhortensie", "Hydrangea macrophylla"),
    ("Gewoehnlicher Buchsbaum", "Buxus sempervirens"),
    ("Europaeische Eibe", "Taxus baccata"),
    ("Gewoehnlicher Efeu", "Hedera helix"),
    ("Gewoehnliche Waldrebe", "Clematis vitalba"),
    ("Himbeere", "Rubus idaeus"),
    ("Schnittlauch", "Allium schoenoprasum"),
    ("Basilikum", "Ocimum basilicum"),
    ("Moehre", "Daucus carota subsp. sativus"),
    ("Weisse Seerose", "Nymphaea alba"),
    ("Spitz-Ahorn", "Acer platanoides"),
    ("Chinaschilf", "Miscanthus sinensis"),
    ("Roter Sonnenhut", "Echinacea purpurea"),
    ("Wald-Erdbeere", "Fragaria vesca"),
    ("Rhabarber", "Rheum rhabarbarum"),
]

# Wikidata-Eigenschaften, deren Vorhandensein wir zaehlen
INTERESSANT = {
    "P225": "wissenschaftlicher Name",
    "P171": "uebergeordnetes Taxon (Gattung)",
    "P105": "Rang",
    "P1843": "Trivialname",
    "P9714": "Verbreitungsgebiet",
    "P18": "Bild",
}


def abfragen(url):
    """Ruft eine Adresse ab und gibt die Antwort als Python-Daten zurueck."""
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as antwort:
        daten = json.loads(antwort.read().decode("utf-8"))
    time.sleep(PAUSE)
    return daten


def wikidata_suche(latein):
    url = "https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
        "action": "wbsearchentities", "search": latein, "language": "en",
        "type": "item", "limit": 7, "format": "json",
    })
    return [t["id"] for t in abfragen(url).get("search", [])]


def wikidata_entitaeten(ids, props="labels|aliases|claims|sitelinks"):
    if not ids:
        return {}
    url = "https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
        "action": "wbgetentities", "ids": "|".join(ids), "props": props,
        "languages": "de|en", "sitefilter": "dewiki", "format": "json",
    })
    return abfragen(url).get("entities", {})


def erste_aussage(entitaet, prop):
    """Gibt den ersten Wert einer Wikidata-Aussage zurueck (oder None)."""
    for aussage in entitaet.get("claims", {}).get(prop, []):
        wert = aussage.get("mainsnak", {}).get("datavalue", {}).get("value")
        if wert is not None:
            return wert
    return None


def wikipedia_text(titel):
    url = "https://de.wikipedia.org/w/api.php?" + urllib.parse.urlencode({
        "action": "query", "prop": "extracts|revisions", "explaintext": 1,
        "exsectionformat": "wiki", "rvprop": "ids", "redirects": 1,
        "titles": titel, "format": "json",
    })
    seiten = abfragen(url).get("query", {}).get("pages", {})
    for seite in seiten.values():
        revs = seite.get("revisions", [])
        return {
            "titel": seite.get("title"),
            "revision": revs[0].get("revid") if revs else None,
            "text": seite.get("extract", ""),
        }
    return {"titel": titel, "revision": None, "text": ""}


def verarbeite(deutsch, latein):
    ergebnis = {"deutsch_liste": deutsch, "latein_liste": latein, "fehler": []}
    try:
        kandidaten = wikidata_suche(latein)
        entitaeten = wikidata_entitaeten(kandidaten)
        treffer = None
        for eid in kandidaten:
            e = entitaeten.get(eid, {})
            if erste_aussage(e, "P225") == latein:
                treffer = (eid, e)
                break
        ergebnis["kandidaten"] = kandidaten
        if not treffer:
            ergebnis["fehler"].append("Kein Wikidata-Eintrag mit genau diesem lateinischen Namen (P225) gefunden")
            return ergebnis
        eid, e = treffer
        ergebnis["wikidata_id"] = eid
        ergebnis["label_de"] = e.get("labels", {}).get("de", {}).get("value")
        ergebnis["aliase_de"] = [a["value"] for a in e.get("aliases", {}).get("de", [])]
        ergebnis["trivialnamen"] = [
            v for v in (
                a.get("mainsnak", {}).get("datavalue", {}).get("value")
                for a in e.get("claims", {}).get("P1843", [])
            ) if v
        ]
        ergebnis["vorhandene_angaben"] = {
            INTERESSANT[p]: (p in e.get("claims", {})) for p in INTERESSANT
        }
        eltern = erste_aussage(e, "P171")
        if isinstance(eltern, dict) and "id" in eltern:
            ee = wikidata_entitaeten([eltern["id"]], props="labels|claims")
            ergebnis["gattung_id"] = eltern["id"]
            ergebnis["gattung_label"] = (
                ee.get(eltern["id"], {}).get("labels", {}).get("de", {}).get("value")
                or ee.get(eltern["id"], {}).get("labels", {}).get("en", {}).get("value")
            )
        dewiki = e.get("sitelinks", {}).get("dewiki", {}).get("title")
        ergebnis["dewiki_titel"] = dewiki
        if dewiki:
            ergebnis["wikipedia"] = wikipedia_text(dewiki)
        else:
            ergebnis["fehler"].append("Kein Artikel in der deutschen Wikipedia verknuepft")
    except Exception as fehler:  # bewusst breit: ein Fehler soll nicht alles abbrechen
        ergebnis["fehler"].append(f"{type(fehler).__name__}: {fehler}")
    return ergebnis


def main():
    alle = []
    for nummer, (deutsch, latein) in enumerate(PFLANZEN, start=1):
        print(f"[{nummer:2d}/{len(PFLANZEN)}] {deutsch} ({latein}) ...", flush=True)
        alle.append(verarbeite(deutsch, latein))
        if alle[-1]["fehler"]:
            print("      Hinweis:", "; ".join(alle[-1]["fehler"]), flush=True)
    with open("probe_ergebnis.json", "w", encoding="utf-8") as datei:
        json.dump(
            {"erstellt": time.strftime("%Y-%m-%d %H:%M:%S"), "pflanzen": alle},
            datei, ensure_ascii=False, indent=1,
        )
    print("\nFertig. Datei probe_ergebnis.json wurde erstellt.")


if __name__ == "__main__":
    sys.exit(main())
