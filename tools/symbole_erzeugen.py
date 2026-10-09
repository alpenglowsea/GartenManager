#!/usr/bin/env python3
"""Erzeugt die Zeichenbefehle der 13 Pflanzen-Piktogramme als Kotlin-Datei.

Aufruf:  python3 tools/symbole_erzeugen.py [--vorschau datei.png]

Jedes Piktogramm ist eine Folge einfacher Formen auf einer Fläche von 0 bis 100:
  K cx cy rx ry F        Ellipse
  V F x y x y ...        gefülltes Vieleck
  L F breite x y x y ... Linienzug mit runden Enden
F ist eine Farbmarke: W (weiß), W85, W75, W70 (abgedunkeltes Weiß) oder A (Akzentfarbe,
dunkler als die Gruppenfarbe). Die App liest diese Zeilen und zeichnet sie; so gibt es keine
Bilddateien und die Symbole lassen sich in jeder Größe und Farbe zeichnen.
Nur Python-Standardbibliothek (die Vorschau braucht zusätzlich Pillow).
"""
import math
import os
import sys

ZIEL = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "java", "io", "github", "alpenglowsea",
                    "gartenmanager", "ui", "PflanzenSymbolDaten.kt")


def f(x):
    s = f"{x:.1f}"
    return s[:-2] if s.endswith(".0") else s


class Rekorder:
    def __init__(self):
        self.zeilen = []

    def ell(self, cx, cy, rx, ry, farbe):
        self.zeilen.append(f"K {f(cx)} {f(cy)} {f(rx)} {f(ry)} {farbe}")

    def poly(self, pts, farbe):
        self.zeilen.append("V " + farbe + " " + " ".join(f"{f(x)} {f(y)}" for x, y in pts))

    def line(self, pts, w, farbe):
        self.zeilen.append("L " + farbe + " " + f(w) + " " + " ".join(f"{f(x)} {f(y)}" for x, y in pts))

    def leaf(self, x, y, ang, l, w, farbe, n=10):
        pts = [(t / n * l, math.sin(t / n * math.pi) * w / 2) for t in range(n + 1)]
        pts += [(t / n * l, -math.sin(t / n * math.pi) * w / 2) for t in range(n, -1, -1)]
        a = math.radians(ang)
        self.poly([(x + px * math.cos(a) - py * math.sin(a), y + px * math.sin(a) + py * math.cos(a)) for px, py in pts], farbe)


W, W85, W75, W70, A = "W", "W85", "W75", "W70", "A"


def stauden(g):
    for x, top in ((32, 30), (50, 20), (68, 32)):
        g.line([(x, 78), (x, top + 6)], 4, W)
        for k in range(6):
            g.leaf(x, top, k * 60, 11, 8, W)
        g.ell(x, top, 3.5, 3.5, A)
    g.leaf(50, 78, -150, 14, 7, W); g.leaf(50, 78, -30, 14, 7, W)


def straucher(g):
    g.line([(50, 84), (50, 62)], 5, W); g.line([(42, 84), (50, 70)], 3, W); g.line([(58, 84), (50, 70)], 3, W)
    for cx, cy, r in ((34, 54, 15), (66, 54, 15), (50, 38, 17), (50, 56, 17)):
        g.ell(cx, cy, r, r, W)
    for cx, cy in ((42, 46), (58, 44), (50, 58), (36, 58)):
        g.ell(cx, cy, 2.2, 2.2, A)


def graeser(g):
    for ang, l in ((-46, 50), (-26, 58), (-8, 64), (10, 62), (28, 56), (46, 48)):
        r = math.radians(ang)
        pts = []
        for i in range(0, 13):
            t = i / 12
            pts.append((50 + math.sin(r) * l * t * 0.9 + 10 * (1 if ang > 0 else -1) * t * t * 0.6, 86 - math.cos(r) * l * t))
        g.line(pts, 4.5, W)


def kletter(g):
    g.line([(32, 88), (32, 14)], 3.5, W85); g.line([(68, 88), (68, 14)], 3.5, W85)
    for y in range(24, 86, 14):
        g.line([(32, y), (68, y)], 2.5, W75)
    pts = [(50 + 14 * math.sin(i / 40 * 4.5 * math.pi), 86 - i / 40 * 70) for i in range(0, 41)]
    g.line(pts, 4, W)
    for (x, y, ang) in ((30, 66, 200), (70, 52, -20), (30, 38, 200), (70, 26, -20)):
        g.leaf(x, y, ang, 12, 8, W)


def hecke(g):
    g.poly([(14, 80), (14, 38)] + [(14 + 72 * i / 20, 38 - 8 * math.sin(i / 20 * 3 * math.pi)) for i in range(0, 21)] + [(86, 80)], W)
    for x in (22, 38, 54, 70):
        for y in (50, 62, 72):
            g.ell(x + (6 if y == 62 else 0), y, 3, 3, A)


def obst(g):
    g.poly([(45, 90), (55, 90), (53, 58), (47, 58)], W)
    for cx, cy, r in ((34, 44, 17), (66, 44, 17), (50, 30, 19), (50, 48, 19)):
        g.ell(cx, cy, r, r, W)
    for cx, cy in ((40, 40), (60, 36), (50, 54), (34, 52), (64, 52)):
        g.ell(cx, cy, 4.6, 4.6, A)
        g.line([(cx, cy - 4), (cx + 1, cy - 8)], 1.6, W70)


def beeren(g):
    g.line([(50, 88), (50, 66)], 4, W)
    for cx, cy, r in ((32, 58, 14), (68, 58, 14), (50, 48, 16)):
        g.ell(cx, cy, r, r, W)
    for cx, cy in ((30, 56), (40, 66), (50, 46), (60, 62), (68, 54), (56, 36), (42, 40)):
        g.ell(cx, cy, 4, 4, A); g.ell(cx - 1.2, cy - 1.2, 1.2, 1.2, W)


def kraeuter(g):
    g.line([(50, 90), (50, 18)], 4, W)
    for y in (30, 44, 58, 72):
        s = 1.0 - (y - 30) / 120
        g.leaf(50, y, -50, 24 * s, 12 * s, W)
        g.leaf(50, y + 6, 230, 24 * s, 12 * s, W)
    g.leaf(50, 18, -90, 14, 9, W)


def gemuese(g):
    g.poly([(36, 40), (64, 40), (50, 92)], W)
    for y in (52, 64, 76):
        w = 8 - (y - 52) * 0.2
        g.line([(50 - w, y), (50 - w + 7, y)], 2, A)
    for ang in (-115, -90, -65):
        g.leaf(50, 40, ang, 28, 11, W)


def blumen(g):
    g.line([(50, 56), (50, 92)], 4, W)
    g.leaf(50, 80, 200, 20, 10, W); g.leaf(50, 72, -20, 20, 10, W)
    for k in range(6):
        g.leaf(50, 36, k * 60 - 90, 20, 15, W)
    g.ell(50, 36, 7, 7, A)


def wasser(g):
    pts = [(50, 56)] + [(50 + 36 * math.cos(math.radians(20 + i)), 56 + 17 * math.sin(math.radians(20 + i))) for i in range(0, 321, 8)]
    pts.append((50, 56))
    g.poly(pts, W)
    for k in range(5):
        g.leaf(50, 40, k * 36 - 162, 13, 8, W)
    g.ell(50, 38, 4, 4, A)
    for y, x1 in ((78, 22), (88, 28)):
        g.line([(x1 + 9 * i, y + (3 if i % 2 else -3)) for i in range(0, 6)], 3, W)


def baum(g):
    g.poly([(44, 92), (56, 92), (53, 60), (47, 60)], W)
    for cx, cy, r in ((30, 42, 18), (70, 42, 18), (50, 24, 20), (50, 44, 24)):
        g.ell(cx, cy, r, r, W)
    g.line([(50, 70), (40, 56)], 3, A); g.line([(50, 66), (60, 54)], 3, A)


def zwiebel(g):
    g.line([(50, 56), (50, 76)], 4, W)
    g.leaf(50, 74, 205, 26, 10, W); g.leaf(50, 74, -25, 26, 10, W)
    g.poly([(32, 20), (40, 34), (50, 18), (60, 34), (68, 20), (70, 44), (62, 58), (38, 58), (30, 44)], W)
    g.poly([(50, 94), (38, 84), (42, 78), (50, 74), (58, 78), (62, 84)], W)


# Name der Gruppe (wie in daten/gruppen.json), Gruppenfarbe, Zeichenfunktion
GRUPPEN = [
    ("Stauden", 0x5A9A3C, stauden),
    ("Sträucher", 0x2F7D55, straucher),
    ("Bäume", 0x6E5A3C, baum),
    ("Heckenpflanzen", 0x1F4D33, hecke),
    ("Gräser", 0xC2C84A, graeser),
    ("Kletterpflanzen", 0x4FB5A5, kletter),
    ("Obstgehölze", 0xC85A46, obst),
    ("Beerensträucher", 0x9C3B66, beeren),
    ("Kräuter", 0x8FD16A, kraeuter),
    ("Gemüsepflanzen", 0xD68232, gemuese),
    ("Blumen", 0xE58AB8, blumen),
    ("Wasserpflanzen", 0x3C82BE, wasser),
    ("Zwiebel- und Knollenblumen", 0x965AB4, zwiebel),
]


def schreibe():
    out = ["package io.github.alpenglowsea.gartenmanager.ui", "",
           "// ERZEUGT von tools/symbole_erzeugen.py - nicht von Hand ändern.", "",
           "/** Name der Gruppe, Gruppenfarbe (RGB) und Zeichenbefehle des Piktogramms (Format siehe Skript). */",
           "internal class SymbolRohdaten(val gruppe: String, val farbe: Int, val befehle: String)", "",
           "internal val SYMBOL_ROHDATEN: List<SymbolRohdaten> = listOf("]
    for name, farbe, fn in GRUPPEN:
        g = Rekorder()
        fn(g)
        out.append(f'    SymbolRohdaten("{name}", 0x{farbe:06X}, """')
        out += ["        " + z for z in g.zeilen]
        out.append('    """.trimIndent()),')
    out.append(")")
    os.makedirs(os.path.dirname(ZIEL), exist_ok=True)
    with open(ZIEL, "w", encoding="utf-8") as fh:
        fh.write("\n".join(out) + "\n")
    print("Geschrieben:", ZIEL, f"({os.path.getsize(ZIEL)} Bytes)")


def vorschau(pfad):
    """Liest die erzeugte Kotlin-Datei wieder ein (wie die App) und zeichnet sie mit Pillow."""
    from PIL import Image, ImageDraw
    import re
    text = open(ZIEL, encoding="utf-8").read()
    SS, D = 4, 150
    bild = Image.new("RGB", (5 * D * SS, 3 * D * SS), (250, 248, 242))
    d = ImageDraw.Draw(bild)
    grau = {"W": 255, "W85": 216, "W75": 191, "W70": 178}
    for i, m in enumerate(re.finditer(r'SymbolRohdaten\("([^"]+)", 0x([0-9A-F]{6}), """\n(.*?)\n\s*"""', text, re.S)):
        name, hexf, befehle = m.group(1), int(m.group(2), 16), m.group(3)
        col = ((hexf >> 16) & 255, (hexf >> 8) & 255, hexf & 255)
        acc = tuple(int(c * 0.55) for c in col)
        cx = (i % 5 + 0.5) * D * SS
        cy = (i // 5 + 0.5) * D * SS
        r = D * SS * 0.42
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col, outline=tuple(int(c * 0.6) for c in col), width=SS * 2)
        gs = 2 * r * 0.84
        x0, y0, s = cx - gs / 2, cy - gs / 2, gs / 100

        def farbe(t):
            return acc if t == "A" else (grau[t],) * 3
        P = lambda x, y: (x0 + float(x) * s, y0 + float(y) * s)
        for zeile in befehle.splitlines():
            t = zeile.split()
            if t[0] == "K":
                a = P(float(t[1]) - float(t[3]), float(t[2]) - float(t[4])); b = P(float(t[1]) + float(t[3]), float(t[2]) + float(t[4]))
                d.ellipse([a, b], fill=farbe(t[5]))
            elif t[0] == "V":
                z = list(map(float, t[2:]))
                d.polygon([P(z[k], z[k + 1]) for k in range(0, len(z), 2)], fill=farbe(t[1]))
            elif t[0] == "L":
                w = float(t[2]) * s
                z = list(map(float, t[3:]))
                q = [P(z[k], z[k + 1]) for k in range(0, len(z), 2)]
                d.line(q, fill=farbe(t[1]), width=max(1, int(w)), joint="curve")
                for (x, y) in (q[0], q[-1]):
                    d.ellipse([x - w / 2, y - w / 2, x + w / 2, y + w / 2], fill=farbe(t[1]))
    bild.resize((5 * D, 3 * D), Image.LANCZOS).save(pfad)
    print("Vorschau:", pfad)


if __name__ == "__main__":
    schreibe()
    if "--vorschau" in sys.argv:
        vorschau(sys.argv[sys.argv.index("--vorschau") + 1])
