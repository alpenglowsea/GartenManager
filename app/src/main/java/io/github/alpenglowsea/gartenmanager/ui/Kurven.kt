package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/** Ein Stück der Kurve von Punkt [von] zum nächsten Punkt, als Bézier-Kurve mit Führungspunkten c1 und c2. */
class KurvenSegment(val von: Int, val p1: Offset, val c1: Offset, val c2: Offset, val p2: Offset)

/**
 * Kurve durch die Punkte (Catmull-Rom, als Bézier-Stücke). Ein Punkt mit rund = false
 * ist eine Ecke (dort keine weiche Führung). Die Führung wird auf 40 Prozent der Länge
 * des Teilstücks begrenzt, damit die Kurve bei sehr ungleichen Abständen nicht so
 * stark überschießt.
 */
fun kurvenSegmente(p: List<Offset>, rund: List<Boolean>, geschlossen: Boolean): List<KurvenSegment> {
    val n = p.size
    if (n < 2) return emptyList()
    val anzahl = if (geschlossen) n else n - 1
    return List(anzahl) { i ->
        val i2 = (i + 1) % n
        val p1 = p[i]
        val p2 = p[i2]
        val p0 = if (geschlossen) p[(i - 1 + n) % n] else p[maxOf(i - 1, 0)]
        val p3 = if (geschlossen) p[(i + 2) % n] else p[minOf(i + 2, n - 1)]
        val grenze = (p2 - p1).getDistance() * 0.4f
        val c1 = if (rund[i]) p1 + begrenze((p2 - p0) / 6f, grenze) else p1
        val c2 = if (rund[i2]) p2 - begrenze((p3 - p1) / 6f, grenze) else p2
        KurvenSegment(i, p1, c1, c2, p2)
    }
}

fun kurvenPfad(p: List<Offset>, rund: List<Boolean>, geschlossen: Boolean): Path {
    val pfad = Path()
    if (p.isEmpty()) return pfad
    pfad.moveTo(p[0].x, p[0].y)
    for (s in kurvenSegmente(p, rund, geschlossen)) {
        pfad.cubicTo(s.c1.x, s.c1.y, s.c2.x, s.c2.y, s.p2.x, s.p2.y)
    }
    if (geschlossen && p.size >= 2) pfad.close()
    return pfad
}

private fun begrenze(v: Offset, maximum: Float): Offset {
    val laenge = v.getDistance()
    return if (laenge > maximum && laenge > 0f) v * (maximum / laenge) else v
}

private fun bezier(s: KurvenSegment, t: Float): Offset {
    val u = 1f - t
    return s.p1 * (u * u * u) + s.c1 * (3f * u * u * t) + s.c2 * (3f * u * t * t) + s.p2 * (t * t * t)
}

/** Die Kurve als Linienzug aus vielen kurzen Stücken (für Treffer-Prüfungen). */
fun kurvenPolylinie(p: List<Offset>, rund: List<Boolean>, geschlossen: Boolean, schritte: Int = 12): List<Offset> {
    if (p.isEmpty()) return emptyList()
    val ergebnis = ArrayList<Offset>()
    ergebnis.add(p[0])
    for (s in kurvenSegmente(p, rund, geschlossen)) {
        for (k in 1..schritte) ergebnis.add(bezier(s, k.toFloat() / schritte))
    }
    return ergebnis
}

/** Liegt [pos] im Inneren des Vielecks? (Strahlverfahren) */
fun liegtInnen(polygon: List<Offset>, pos: Offset): Boolean {
    var innen = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > pos.y) != (b.y > pos.y) &&
            pos.x < (b.x - a.x) * (pos.y - a.y) / (b.y - a.y) + a.x
        ) {
            innen = !innen
        }
        j = i
    }
    return innen
}

class KurvenTreffer(val segment: Int, val punkt: Offset, val abstand: Float)

private fun naechsterPunktAufStrecke(a: Offset, b: Offset, pos: Offset): Offset {
    val ab = b - a
    val laengeQuadrat = ab.x * ab.x + ab.y * ab.y
    if (laengeQuadrat == 0f) return a
    val t = (((pos.x - a.x) * ab.x + (pos.y - a.y) * ab.y) / laengeQuadrat).coerceIn(0f, 1f)
    return a + ab * t
}

/** Der Punkt auf der Kurve, der [pos] am nächsten liegt, samt Nummer des Teilstücks. */
fun naechsterKurvenPunkt(p: List<Offset>, rund: List<Boolean>, geschlossen: Boolean, pos: Offset, schritte: Int = 16): KurvenTreffer? {
    var beste: KurvenTreffer? = null
    for (s in kurvenSegmente(p, rund, geschlossen)) {
        var a = s.p1
        for (k in 1..schritte) {
            val b = bezier(s, k.toFloat() / schritte)
            val q = naechsterPunktAufStrecke(a, b, pos)
            val d = (q - pos).getDistance()
            val bisher = beste
            if (bisher == null || d < bisher.abstand) beste = KurvenTreffer(s.von, q, d)
            a = b
        }
    }
    return beste
}
