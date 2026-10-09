#!/usr/bin/env python3
"""Macht aus den heruntergeladenen Bildern die Bilder der App.

Aufruf (im Hauptordner des Projekts):
    python3 tools/baue_bilder.py                      # Standard: längste Kante 640 Pixel
    python3 tools/baue_bilder.py --kante 480          # kleiner (spart App-Größe)

Eingabe:  rohdaten/bilder.json und rohdaten/bilder/*   (von hole_bilder.py)
          daten/bilder_eigen/eigene_bilder.json        (optional, eigene Fotos, siehe unten)
Ausgabe:  app/src/main/assets/bilder/<name>.webp       die Bilder der App
          daten/bilder.json                             Urheber, Lizenz, Quelle je Bild (kommt ins Git)

Danach: baue_grunddaten.py neu laufen lassen (und daten/version.txt erhöhen).

Eigene Fotos: Datei in daten/bilder_eigen/ legen und in eigene_bilder.json eintragen:
    {"dahlia-x-hybrida": {"datei": "dahlie.jpg", "urheber": "Dein Name", "lizenz": "CC BY-SA 4.0",
                           "lizenz_url": "https://creativecommons.org/licenses/by-sa/4.0/", "titel": "Eigenes Foto"}}
Der Schlüssel ist der Name aus rohdaten/bilder_bericht.md (die Klammer hinter dem Pflanzennamen).
Ein eigenes Foto hat Vorrang vor einem Bild von Commons.

Braucht Pillow (sudo apt install python3-pil).
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pflanzenliste import WURZEL  # noqa: E402

try:
    from PIL import Image, ImageOps
except ImportError:
    sys.exit("FEHLER: Pillow fehlt. Bitte installieren:  sudo apt install python3-pil")


def konvertiere(quelle, ziel, kante, qualitaet):
    with Image.open(quelle) as im:
        im = ImageOps.exif_transpose(im)
        if im.mode in ("RGBA", "LA", "P"):
            im = im.convert("RGBA")
            hintergrund = Image.new("RGB", im.size, (255, 255, 255))
            hintergrund.paste(im, mask=im.split()[-1])
            im = hintergrund
        else:
            im = im.convert("RGB")
        im.thumbnail((kante, kante), Image.LANCZOS)
        im.save(ziel, "WEBP", quality=qualitaet, method=6)
        return im.size


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kante", type=int, default=640, help="längste Kante in Pixel (Standard 640)")
    ap.add_argument("--qualitaet", type=int, default=75, help="WebP-Qualität 1-100 (Standard 75)")
    ap.add_argument("--wurzel", default=WURZEL)
    args = ap.parse_args()

    roh = os.path.join(args.wurzel, "rohdaten")
    daten = os.path.join(args.wurzel, "daten")
    ziel_ordner = os.path.join(args.wurzel, "app", "src", "main", "assets", "bilder")
    os.makedirs(ziel_ordner, exist_ok=True)

    pfad_json = os.path.join(roh, "bilder.json")
    if not os.path.exists(pfad_json):
        sys.exit("FEHLER: rohdaten/bilder.json fehlt. Bitte zuerst hole_bilder.py laufen lassen.")
    bilder = json.load(open(pfad_json, encoding="utf-8"))

    quellen = {}  # name -> (Pfad, Metadaten)
    for k, v in bilder.items():
        if v.get("datei"):
            p = os.path.join(roh, "bilder", v["datei"])
            if not os.path.exists(p):
                print(f"WARNUNG: Datei fehlt für {k}: {v['datei']}")
                continue
            quellen[k] = (p, {
                "urheber": v.get("urheber", ""), "lizenz": v["lizenz"], "lizenz_url": v.get("lizenz_url", ""),
                "seite": v.get("seite", ""), "titel": v.get("commons_titel", ""), "herkunft": v.get("herkunft", ""),
                "abruf": v.get("abruf", ""),
            })
    eigen_pfad = os.path.join(daten, "bilder_eigen", "eigene_bilder.json")
    eigene = 0
    if os.path.exists(eigen_pfad):
        for k, v in json.load(open(eigen_pfad, encoding="utf-8")).items():
            p = os.path.join(daten, "bilder_eigen", v["datei"])
            if not os.path.exists(p):
                sys.exit(f"FEHLER: eigenes Bild fehlt: {p}")
            quellen[k] = (p, {
                "urheber": v.get("urheber", ""), "lizenz": v["lizenz"], "lizenz_url": v.get("lizenz_url", ""),
                "seite": v.get("seite", ""), "titel": v.get("titel", "Eigenes Foto"), "herkunft": "eigenes Foto",
                "abruf": v.get("abruf", ""),
            })
            eigene += 1

    ergebnis = {}
    summe = 0
    for k in sorted(quellen):
        p, meta = quellen[k]
        name = k + ".webp"
        ziel = os.path.join(ziel_ordner, name)
        b, h = konvertiere(p, ziel, args.kante, args.qualitaet)
        summe += os.path.getsize(ziel)
        meta.update({"datei": name, "breite": b, "hoehe": h})
        ergebnis[k] = meta
    for alt in os.listdir(ziel_ordner):
        if alt.endswith(".webp") and alt[:-5] not in ergebnis:
            os.remove(os.path.join(ziel_ordner, alt))
            print("Altes Bild entfernt:", alt)

    with open(os.path.join(daten, "bilder.json"), "w", encoding="utf-8") as f:
        json.dump(ergebnis, f, ensure_ascii=False, indent=1, sort_keys=True)
    n = len(ergebnis)
    print(f"Fertig: {n} Bilder ({eigene} eigene), zusammen {summe / 1024 / 1024:.1f} MB, "
          f"Durchschnitt {summe // max(1, n) // 1024} KB. Kante {args.kante} px, Qualität {args.qualitaet}.")
    print("Als Nächstes: daten/version.txt erhöhen und  python3 tools/baue_grunddaten.py")


if __name__ == "__main__":
    main()
