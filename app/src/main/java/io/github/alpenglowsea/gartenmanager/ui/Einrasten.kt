package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Ergebnis der Ausrichtungshilfe: der (eventuell verschobene) Punkt und die
 * Hilfslinien, an denen er eingerastet ist (jeweils von einem Bezugspunkt bis zum Punkt).
 */
class EinrastErgebnis(
    val punkt: Offset,
    val hilfslinien: List<Pair<Offset, Offset>>,
    /** Stelle einer fremden Ecke oder Kante, an der der Punkt eingerastet ist (für die Markierung). */
    val marke: Offset? = null,
)

private class Linie(val ursprung: Offset, val richtung: Offset, val abstand: Float)

private fun kreuz(a: Offset, b: Offset): Float = a.x * b.y - a.y * b.x

/**
 * Ausrichtungshilfe beim Zeichnen. Ein neuer Punkt [roh] rastet ein, wenn er nah an
 * einer dieser Linien liegt:
 *  - waagerecht oder senkrecht durch einen der schon gesetzten [punkte],
 *  - in Verlängerung der letzten Kante,
 *  - im rechten Winkel zur letzten Kante (am letzten Punkt).
 * Liegen zwei nicht parallele Linien in Reichweite, rastet der Punkt an ihrem
 * Schnittpunkt ein (so entstehen saubere Rechtecke). [schwelle] ist die Reichweite
 * in Skizzeneinheiten.
 */
fun rastePunktEin(
    roh: Offset,
    punkte: List<Offset>,
    schwelle: Float,
    mitKante: Boolean = true,
    zusatz: List<Offset> = emptyList(),
): EinrastErgebnis {
    if (punkte.isEmpty() && zusatz.isEmpty()) return EinrastErgebnis(roh, emptyList())

    val kandidaten = ArrayList<Pair<Offset, Offset>>() // Ursprung und Richtung (Länge 1)
    for (p in punkte + zusatz) {
        kandidaten.add(p to Offset(1f, 0f))
        kandidaten.add(p to Offset(0f, 1f))
    }
    if (mitKante && punkte.size >= 2) {
        val letzter = punkte[punkte.size - 1]
        val vorletzter = punkte[punkte.size - 2]
        val kante = letzter - vorletzter
        val laenge = kante.getDistance()
        if (laenge > 0f) {
            val u = kante / laenge
            kandidaten.add(letzter to u)
            kandidaten.add(letzter to Offset(-u.y, u.x))
        }
    }

    val nah = kandidaten
        .map { (ursprung, richtung) -> Linie(ursprung, richtung, abs(kreuz(roh - ursprung, richtung))) }
        .filter { it.abstand < schwelle }
        .sortedBy { it.abstand }
    if (nah.isEmpty()) return EinrastErgebnis(roh, emptyList())

    val erste = nah[0]
    for (zweite in nah.drop(1)) {
        val q = kreuz(erste.richtung, zweite.richtung)
        if (abs(q) > 0.2f) {
            val t = kreuz(zweite.ursprung - erste.ursprung, zweite.richtung) / q
            val schnitt = erste.ursprung + erste.richtung * t
            if ((schnitt - roh).getDistance() < schwelle * 2.5f) {
                return EinrastErgebnis(
                    schnitt,
                    listOf(erste.ursprung to schnitt, zweite.ursprung to schnitt),
                )
            }
        }
    }
    val d = roh - erste.ursprung
    val fuss = erste.ursprung + erste.richtung * (d.x * erste.richtung.x + d.y * erste.richtung.y)
    return EinrastErgebnis(fuss, listOf(erste.ursprung to fuss))
}

/** Ecken und Kanten der anderen Flächen und Gegenstände (Skizzenkoordinaten), an denen etwas einrasten kann. */
class FremdZiele(val ecken: List<Offset>, val kanten: List<Pair<Offset, Offset>>)

/** Ergebnis des Einrastens beim Verschieben: die verschobene Verschiebung und die Stellen, an denen es einrastet. */
class BewegungsErgebnis(val delta: Offset, val marken: List<Offset>)

private fun naechsteEcke(p: Offset, ecken: List<Offset>, schwelle: Float): Offset? {
    var beste: Offset? = null
    var d0 = schwelle
    for (e in ecken) {
        val d = (e - p).getDistance()
        if (d < d0) {
            d0 = d
            beste = e
        }
    }
    return beste
}

/** Der nächste Punkt auf einer der Kanten, wenn er näher als [schwelle] liegt. */
private fun naechsterAufKante(p: Offset, kanten: List<Pair<Offset, Offset>>, schwelle: Float): Offset? {
    var beste: Offset? = null
    var d0 = schwelle
    for ((a, b) in kanten) {
        val ab = b - a
        val l2 = ab.x * ab.x + ab.y * ab.y
        if (l2 <= 0f) continue
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / l2).coerceIn(0f, 1f)
        val q = a + ab * t
        val d = (q - p).getDistance()
        if (d < d0) {
            d0 = d
            beste = q
        }
    }
    return beste
}

/**
 * Wie [rastePunktEin], rastet aber zusätzlich an anderen Flächen und Gegenständen ein:
 * 1. genau auf einer fremden Ecke, 2. an den eigenen Hilfslinien, 3. auf einer fremden Kante,
 * 4. waagerecht oder senkrecht in einer Flucht mit einer fremden Ecke.
 */
fun rastePunktMitFremd(
    roh: Offset,
    eigene: List<Offset>,
    schwelle: Float,
    mitKante: Boolean,
    fremd: FremdZiele?,
): EinrastErgebnis {
    if (fremd == null) return rastePunktEin(roh, eigene, schwelle, mitKante)
    val ecke = naechsteEcke(roh, fremd.ecken, schwelle * 1.5f)
    if (ecke != null) return EinrastErgebnis(ecke, emptyList(), ecke)
    val eigen = rastePunktEin(roh, eigene, schwelle, mitKante)
    if (eigen.hilfslinien.isNotEmpty()) return eigen
    val kante = naechsterAufKante(roh, fremd.kanten, schwelle)
    if (kante != null) return EinrastErgebnis(kante, emptyList(), kante)
    return rastePunktEin(roh, eigene, schwelle, mitKante, fremd.ecken)
}

/**
 * Einrasten beim Verschieben eines ganzen Elements: Seine [ecken] (vor dem Verschieben) um [delta] verschoben;
 * rastet eine davon an einer fremden Ecke oder Kante ein, wird [delta] entsprechend korrigiert.
 * Erst Ecke auf Ecke; sonst Ecke auf Kante und danach (entlang der Kante) noch einmal Ecke auf Ecke.
 */
fun rasteBewegung(ecken: List<Offset>, delta: Offset, fremd: FremdZiele, schwelle: Float): BewegungsErgebnis {
    if (ecken.isEmpty()) return BewegungsErgebnis(delta, emptyList())
    fun eckeAufEcke(d: Offset): BewegungsErgebnis? {
        var best: BewegungsErgebnis? = null
        var d0 = schwelle * 1.5f
        for (e in ecken) {
            val p = e + d
            for (f in fremd.ecken) {
                val abstand = (f - p).getDistance()
                if (abstand < d0) {
                    d0 = abstand
                    best = BewegungsErgebnis(d + (f - p), listOf(f))
                }
            }
        }
        return best
    }
    eckeAufEcke(delta)?.let { return it }
    var bestKorr: Offset? = null
    var bestMarke: Offset? = null
    var d0 = schwelle
    for (e in ecken) {
        val p = e + delta
        val q = naechsterAufKante(p, fremd.kanten, d0) ?: continue
        val abstand = (q - p).getDistance()
        if (abstand < d0) {
            d0 = abstand
            bestKorr = q - p
            bestMarke = q
        }
    }
    if (bestKorr == null || bestMarke == null) return BewegungsErgebnis(delta, emptyList())
    val d2 = delta + bestKorr
    return eckeAufEcke(d2) ?: BewegungsErgebnis(d2, listOf(bestMarke))
}
