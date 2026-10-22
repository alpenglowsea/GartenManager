package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import io.github.alpenglowsea.gartenmanager.daten.ART_PFLANZE
import io.github.alpenglowsea.gartenmanager.daten.Ebene
import io.github.alpenglowsea.gartenmanager.daten.Flaeche
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Gegenstand
import io.github.alpenglowsea.gartenmanager.daten.Gegenstandspunkt
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import io.github.alpenglowsea.gartenmanager.daten.Punkt
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Ein Teil der Vorschau: eine Fläche mit ihren Punkten oder ein Gegenstand (mit Punkten, falls ein freies Gebäude). */
private class VorschauElement(
    val flaeche: Flaeche?,
    val punkte: List<Punkt>,
    val gegenstand: Gegenstand?,
    val gegenstandspunkte: List<Gegenstandspunkt>,
)

private class VorschauSzene(
    val elemente: List<VorschauElement>,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
)

private class Roh(val ebene: Int, val reihenfolge: Int, val istGegenstand: Boolean, val id: Long, val element: VorschauElement)

private fun baueSzene(
    flaechen: List<Flaeche>,
    punkte: List<Punkt>,
    gegenstaende: List<Gegenstand>,
    gegenstandspunkte: List<Gegenstandspunkt>,
    ebenen: List<Ebene>,
): VorschauSzene? {
    val rang = ebenen.withIndex().associate { it.value.id to it.index }
    val verborgen = ebenen.filter { it.sicht == 2 }.map { it.id }.toSet()
    val punkteJe = punkte.groupBy { it.flaecheId }.mapValues { e -> e.value.sortedBy { it.nr } }
    val gpJe = gegenstandspunkte.groupBy { it.gegenstandId }.mapValues { e -> e.value.sortedBy { it.nr } }
    val alle = ArrayList<Roh>()
    for (f in flaechen) {
        if (f.ebeneId in verborgen) continue
        val ps = punkteJe[f.id] ?: continue
        if (ps.size < 3) continue
        alle.add(Roh(rang[f.ebeneId] ?: -1, f.reihenfolge, false, f.id, VorschauElement(f, ps, null, emptyList())))
    }
    for (g in gegenstaende) {
        if (g.ebeneId in verborgen) continue
        alle.add(Roh(rang[g.ebeneId] ?: -1, g.reihenfolge, true, g.id, VorschauElement(null, emptyList(), g, gpJe[g.id].orEmpty())))
    }
    if (alle.isEmpty()) return null
    alle.sortWith(compareBy<Roh>({ it.ebene }, { it.reihenfolge }, { it.istGegenstand }, { it.id }))

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (r in alle) {
        val e = r.element
        val f = e.flaeche
        if (f != null) {
            for (p in e.punkte) {
                minX = min(minX, p.x)
                maxX = max(maxX, p.x)
                minY = min(minY, p.y)
                maxY = max(maxY, p.y)
            }
        } else {
            val g = e.gegenstand ?: continue
            val rad = Math.toRadians(g.drehung.toDouble())
            val c = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
            val sn = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()
            val halbX: Float
            val halbY: Float
            when {
                g.art == ART_PFLANZE -> { halbX = g.breite / 2f; halbY = g.breite / 2f }
                e.gegenstandspunkte.isNotEmpty() -> {
                    // Punkte liegen relativ zur Mitte; ob schon gedreht, ist hier unsicher -> vorsichtig mit dem größten Abstand
                    val r = e.gegenstandspunkte.maxOf { hypot(it.x, it.y) }
                    halbX = r; halbY = r
                }
                else -> {
                    // Gedrehtes Rechteck: genaue Ausdehnung statt Umkreis
                    halbX = c * g.breite / 2f + sn * g.hoehe / 2f
                    halbY = sn * g.breite / 2f + c * g.hoehe / 2f
                }
            }
            minX = min(minX, g.mitteX - halbX)
            maxX = max(maxX, g.mitteX + halbX)
            minY = min(minY, g.mitteY - halbY)
            maxY = max(maxY, g.mitteY + halbY)
        }
    }
    return VorschauSzene(alle.map { it.element }, minX, minY, maxX, maxY)
}

/**
 * Kleine Vorschau eines Gartens: Flächen, Gegenstände und Pflanzen verkleinert, so dass alles ins Bild passt.
 * Ein leerer Garten zeigt den Anfangsbuchstaben seines Namens.
 */
@Composable
fun GartenVorschau(garten: Garten, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MeineDatenDb.holen(context).gartenDao() }
    val id = garten.id
    val flaechen by remember(id) { dao.flaechen(id) }.collectAsState(initial = emptyList())
    val punkte by remember(id) { dao.punkte(id) }.collectAsState(initial = emptyList())
    val gegenstaende by remember(id) { dao.gegenstaende(id) }.collectAsState(initial = emptyList())
    val gegenstandspunkte by remember(id) { dao.gegenstandspunkte(id) }.collectAsState(initial = emptyList())
    val ebenen by remember(id) { dao.ebenen(id) }.collectAsState(initial = emptyList())
    val dichte = LocalDensity.current.density
    val grund = MaterialTheme.colorScheme.surfaceVariant
    val schriftFarbe = MaterialTheme.colorScheme.onSurfaceVariant
    val szene = remember(flaechen, punkte, gegenstaende, gegenstandspunkte, ebenen) {
        baueSzene(flaechen, punkte, gegenstaende, gegenstandspunkte, ebenen)
    }
    val massstab = garten.massstab

    Box(modifier.background(grund), contentAlignment = Alignment.Center) {
        if (szene == null) {
            Text(
                text = garten.name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = schriftFarbe,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val breite = max(szene.maxX - szene.minX, 1f)
                val hoehe = max(szene.maxY - szene.minY, 1f)
                val skala = min(size.width * 0.95f / breite, size.height * 0.95f / hoehe)
                val ox = size.width / 2f - (szene.minX + szene.maxX) / 2f * skala
                val oy = size.height / 2f - (szene.minY + szene.maxY) / 2f * skala
                fun zu(x: Float, y: Float) = Offset(ox + x * skala, oy + y * skala)
                for (el in szene.elemente) {
                    val f = el.flaeche
                    if (f != null) {
                        val bild = el.punkte.map { zu(it.x, it.y) }
                        val pfad = kurvenPfad(bild, el.punkte.map { it.rund }, geschlossen = true)
                        val aus = Oberflaeche.vonSchluessel(f.oberflaeche).aussehen(f.farbe)
                        drawPath(pfad, aus.fuellung)
                        drawPath(pfad, aus.rand, style = Stroke(width = dichte))
                        continue
                    }
                    val g = el.gegenstand ?: continue
                    val mitte = zu(g.mitteX, g.mitteY)
                    if (g.art == ART_PFLANZE) {
                        val gruppe = g.pflanzeGruppe.orEmpty()
                        val farbe = g.farbe?.let { Color(it) } ?: gruppenFarbe(gruppe)
                        val r = max(g.breite * skala / 2f, 1.5f * dichte)
                        drawCircle(farbe, radius = r, center = mitte)
                        drawCircle(Color.Black.copy(alpha = 0.25f), radius = r, center = mitte, style = Stroke(width = 0.5f * dichte))
                    } else if (g.art == GegenstandsArt.GEBAEUDE_FREI.schluessel) {
                        zeichneGebaeude(g, el.gegenstandspunkte, mitte, skala, dichte, false, null, 0f, skala * massstab)
                    } else {
                        zeichneGegenstandBild(g, mitte, skala, dichte, false, null, 0f, skala * massstab)
                    }
                }
            }
        }
    }
}
