package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Ergebnis der Ausrichtungshilfe: der (eventuell verschobene) Punkt und die
 * Hilfslinien, an denen er eingerastet ist (jeweils von einem Bezugspunkt bis zum Punkt).
 */
class EinrastErgebnis(val punkt: Offset, val hilfslinien: List<Pair<Offset, Offset>>)

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
fun rastePunktEin(roh: Offset, punkte: List<Offset>, schwelle: Float, mitKante: Boolean = true): EinrastErgebnis {
    if (punkte.isEmpty()) return EinrastErgebnis(roh, emptyList())

    val kandidaten = ArrayList<Pair<Offset, Offset>>() // Ursprung und Richtung (Länge 1)
    for (p in punkte) {
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
