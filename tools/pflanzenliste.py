"""Gemeinsame Hilfen für die Datenbeschaffung: Pflanzenliste lesen, Dateinamen bilden.

Nur Python-Standardbibliothek.
"""
import os
import re
import unicodedata
from collections import Counter

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def finde_liste(wurzel=WURZEL):
    """Sucht Pflanzenliste.md in daten/ oder docs/."""
    for teil in ("daten", "docs"):
        pfad = os.path.join(wurzel, teil, "Pflanzenliste.md")
        if os.path.exists(pfad):
            return pfad
    raise SystemExit("FEHLER: Pflanzenliste.md nicht gefunden (erwartet in daten/ oder docs/).")


def slug(text):
    t = text.lower().replace("×", " x ").replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
    t = unicodedata.normalize("NFD", t)
    t = "".join(c for c in t if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", "-", t).strip("-")


def lies_liste(pfad):
    """Gibt eine Liste von Pflanzen zurück:
    {deutsch, lateinisch, synonyme, gruppen (erste = Hauptgruppe), schluessel}"""
    pflanzen = []
    gruppe = None
    with open(pfad, encoding="utf-8") as f:
        for zeile in f:
            zeile = zeile.rstrip("\n")
            if zeile.startswith("## "):
                gruppe = zeile[3:].strip()
                continue
            if not zeile.startswith("|") or gruppe is None:
                continue
            zellen = [z.strip() for z in zeile.strip().strip("|").split("|")]
            if len(zellen) < 2 or zellen[0].startswith("---") or zellen[0] == "Deutscher Name":
                continue
            deutsch, latein = zellen[0], zellen[1]
            auch = zellen[2] if len(zellen) > 2 else ""
            synonyme = []
            m = re.match(r"^(.*?)\s*\(syn\.\s*(.*?)\)\s*$", latein)
            if m:
                latein = m.group(1).strip()
                synonyme = [s.strip() for s in re.split(r";| oder ", m.group(2)) if s.strip()]
            gruppen = [gruppe] + [g.strip() for g in auch.split(",") if g.strip() and g.strip() != gruppe]
            pflanzen.append({"deutsch": deutsch, "lateinisch": latein, "synonyme": synonyme, "gruppen": gruppen})
    zaehler = Counter(p["lateinisch"] for p in pflanzen)
    gesehen = set()
    for p in pflanzen:
        s = slug(p["lateinisch"])
        if zaehler[p["lateinisch"]] > 1:
            s += "-" + slug(p["deutsch"])
        if s in gesehen:
            raise SystemExit(f"FEHLER: Doppelter Dateiname {s} in der Pflanzenliste")
        gesehen.add(s)
        p["schluessel"] = s
    return pflanzen
