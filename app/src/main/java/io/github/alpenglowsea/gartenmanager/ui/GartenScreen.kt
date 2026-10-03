package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import io.github.alpenglowsea.gartenmanager.daten.Gegenstandspunkt
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.gestures.detectDragGestures
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import io.github.alpenglowsea.gartenmanager.daten.Gegenstand
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.GartenViewModel
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Ebene
import io.github.alpenglowsea.gartenmanager.daten.Flaeche
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Punkt
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val ZOOM_MIN = 0.1f
private const val ZOOM_MAX = 10f
private const val SCHLIESS_RADIUS_DP = 28f
private const val MIN_FORM_DP = 16f
private const val GRIFF_RADIUS_DP = 26f
private const val KANTE_RADIUS_DP = 18f
private const val LUPE_RADIUS_DP = 58f
private val FANG_FARBE = Color(0xFFFF9800)
private const val LUPE_FAKTOR = 2.5f

/** Große Fläche für Ebenen-Zwischenspeicher, damit auch die vergrößerte Lupe nicht abgeschnitten wird. */
private val SZENE_GRENZE = Rect(-50000f, -50000f, 50000f, 50000f)

/** Ein laufendes Ziehen mit einem Finger: einen Punkt ([punktId]) oder die ganze Fläche (punktId = null). */
private data class Ziehen(
    val flaecheId: Long,
    val punktId: Long?,
    val start: Offset,
    val aktuell: Offset,
)

private enum class GModus { VERSCHIEBEN, GROESSE, DREHEN, PUNKT }

/** Ein laufendes Ziehen an einem Gegenstand: Verschieben, Größe ändern (Griff sx/sy) oder Drehen. */
private data class GZiehen(
    val original: Gegenstand,
    val modus: GModus,
    val sx: Int,
    val sy: Int,
    val start: Offset,
    val aktuell: Offset,
    val punktId: Long? = null, // nur bei GModus.PUNKT: der gezogene Punkt eines freien Gebäudes
)

/** Ein Griff am ausgewählten Gegenstand (Position auf dem Bildschirm). */
private data class GriffInfo(
    val sx: Int,
    val sy: Int,
    val drehen: Boolean,
    val pos: Offset,
    val radius: Float,
    val stiel: Offset? = null, // Ansatzpunkt am Rand, wenn der Griff an einem Stiel sitzt
)

/** Fläche oder Gegenstand in der gemeinsamen Zeichenreihenfolge. */
private data class Element(
    val ebeneId: Long,
    val rang: Int,
    val reihenfolge: Int,
    val id: Long,
    val flaeche: Flaeche?,
    val gegenstand: Gegenstand?,
)

/** Eine Zeile unter einer aufgeklappten Ebene: eine Fläche oder ein Gegenstand. */
private data class ZeilenEintrag(val reihenfolge: Int, val flaeche: Flaeche?, val gegenstand: Gegenstand?)

private fun istGebaeude(g: Gegenstand): Boolean = g.art == GegenstandsArt.GEBAEUDE_FREI.schluessel

/** Einrasten an anderen Formen beim Ziehen eines Gegenstands: Ziele, Reichweite und die eigenen Ecken. */
private class Fang(val ziele: FremdZiele, val schwelle: Float, val eigeneEcken: List<Offset>)

private fun rasteMarken(e: BewegungsErgebnis?): List<Offset> = e?.marken.orEmpty()

/** Ecken eines Gegenstands in Skizzenkoordinaten (bei freien Gebäuden die Punkte). */
private fun gEckenSkizze(g: Gegenstand, ps: List<Gegenstandspunkt>): List<Offset> {
    val w = Math.toRadians(g.drehung.toDouble())
    val c = cos(w).toFloat()
    val s = sin(w).toFloat()
    fun skizze(lx: Float, ly: Float) = Offset(g.mitteX + lx * c - ly * s, g.mitteY + lx * s + ly * c)
    return if (istGebaeude(g)) {
        ps.map { skizze(it.x, it.y) }
    } else {
        val b = g.breite / 2f
        val h = g.hoehe / 2f
        listOf(skizze(-b, -h), skizze(b, -h), skizze(b, h), skizze(-b, h))
    }
}

/** Kanten eines Gegenstands in Skizzenkoordinaten. */
private fun gKantenSkizze(g: Gegenstand, ps: List<Gegenstandspunkt>): List<Pair<Offset, Offset>> {
    val w = Math.toRadians(g.drehung.toDouble())
    val c = cos(w).toFloat()
    val s = sin(w).toFloat()
    fun skizze(p: Offset) = Offset(g.mitteX + p.x * c - p.y * s, g.mitteY + p.x * s + p.y * c)
    val umriss = if (istGebaeude(g)) {
        if (ps.size < 3) return emptyList()
        kurvenPolylinie(ps.map { Offset(it.x, it.y) }, ps.map { it.rund }, true).map { skizze(it) }
    } else {
        gEckenSkizze(g, ps)
    }
    return umriss.indices.map { umriss[it] to umriss[(it + 1) % umriss.size] }
}

/** Ziel eines gezogenen Gebäudepunkts im Koordinatensystem des Gebäudes (mit Einrasten) und das Ergebnis des Einrastens. */
private fun gebaeudePunktZiel(
    g: Gegenstand,
    ps: List<Gegenstandspunkt>,
    z: GZiehen,
    fang: Fang?,
    einrasten: Boolean,
): Pair<Offset, EinrastErgebnis?> {
    val p = ps.firstOrNull { it.id == z.punktId } ?: return Offset.Zero to null
    val d = lokaleVerschiebung(z)
    val lokal = Offset(p.x + d.x, p.y + d.y)
    if (!einrasten || fang == null || ps.size < 3) return lokal to null
    val w = Math.toRadians(g.drehung.toDouble())
    val c = cos(w).toFloat()
    val s = sin(w).toFloat()
    fun skizze(q: Offset) = Offset(g.mitteX + q.x * c - q.y * s, g.mitteY + q.x * s + q.y * c)
    val index = ps.indexOfFirst { it.id == p.id }
    val vor = ps[(index - 1 + ps.size) % ps.size]
    val nach = ps[(index + 1) % ps.size]
    val erg = rastePunktMitFremd(
        skizze(lokal),
        listOf(skizze(Offset(vor.x, vor.y)), skizze(Offset(nach.x, nach.y))),
        fang.schwelle,
        false,
        fang.ziele,
    )
    val dx = erg.punkt.x - g.mitteX
    val dy = erg.punkt.y - g.mitteY
    return Offset(dx * c + dy * s, -dx * s + dy * c) to erg
}

/** Verschiebung des Fingers im Koordinatensystem des (gedrehten) Gegenstands, in Skizzeneinheiten. */
private fun lokaleVerschiebung(z: GZiehen): Offset {
    val d = z.aktuell - z.start
    val w = Math.toRadians(z.original.drehung.toDouble())
    val c = cos(w).toFloat()
    val s = sin(w).toFloat()
    return Offset(d.x * c + d.y * s, -d.x * s + d.y * c)
}

/**
 * Der Gegenstand nach einem laufenden Ziehen. Beim Größe ändern bleibt der gegenüberliegende Griff
 * (Anker) stehen; gerechnet wird im Koordinatensystem des gedrehten Gegenstands.
 */
private fun angepasst(
    z: GZiehen,
    einrasten: Boolean,
    minGroesse: Float,
    fang: Fang? = null,
    marken: MutableList<Offset>? = null,
): Gegenstand {
    val o = z.original
    return when (z.modus) {
        GModus.PUNKT -> o // Beim Ziehen eines Gebäudepunkts bleibt der Gegenstand selbst, nur der Punkt wandert.
        GModus.VERSCHIEBEN -> {
            var d = z.aktuell - z.start
            if (fang != null) {
                val r = rasteBewegung(fang.eigeneEcken, d, fang.ziele, fang.schwelle)
                d = r.delta
                marken?.addAll(r.marken)
            }
            o.copy(mitteX = o.mitteX + d.x, mitteY = o.mitteY + d.y)
        }
        GModus.DREHEN -> {
            val vx = z.aktuell.x - o.mitteX
            val vy = z.aktuell.y - o.mitteY
            var grad = Math.toDegrees(atan2(vx.toDouble(), (-vy).toDouble())).toFloat()
            if (einrasten) grad = (grad / 15f).roundToInt() * 15f
            o.copy(drehung = ((grad % 360f) + 360f) % 360f)
        }
        GModus.GROESSE -> {
            val w = Math.toRadians(o.drehung.toDouble())
            val c = cos(w).toFloat()
            val s = sin(w).toFloat()
            fun lokal(p: Offset): Offset {
                val dx = p.x - o.mitteX
                val dy = p.y - o.mitteY
                return Offset(dx * c + dy * s, -dx * s + dy * c)
            }
            val l0 = lokal(z.start)
            val l1 = lokal(z.aktuell)
            var kante = Offset(z.sx * o.breite / 2f + (l1.x - l0.x), z.sy * o.hoehe / 2f + (l1.y - l0.y))
            if (fang != null) {
                // Die gezogene Kante rastet in einer Flucht mit fremden Ecken ein (im Koordinatensystem des Gegenstands).
                var kx = kante.x
                var ky = kante.y
                var bx = fang.schwelle
                var by = fang.schwelle
                for (f in fang.ziele.ecken) {
                    val lf = lokal(f)
                    if (z.sx != 0 && abs(lf.x - kante.x) < bx) {
                        bx = abs(lf.x - kante.x)
                        kx = lf.x
                        marken?.add(f)
                    }
                    if (z.sy != 0 && abs(lf.y - kante.y) < by) {
                        by = abs(lf.y - kante.y)
                        ky = lf.y
                        marken?.add(f)
                    }
                }
                kante = Offset(kx, ky)
            }
            val anker = Offset(-z.sx * o.breite / 2f, -z.sy * o.hoehe / 2f)
            var nb = o.breite
            var nh = o.hoehe
            if (z.sx != 0 && z.sy != 0) {
                val faktorB = z.sx * (kante.x - anker.x) / o.breite
                val faktorH = z.sy * (kante.y - anker.y) / o.hoehe
                val f = maxOf(faktorB, faktorH, minGroesse / minOf(o.breite, o.hoehe))
                nb = o.breite * f
                nh = o.hoehe * f
            } else if (z.sx != 0) {
                nb = maxOf(minGroesse, z.sx * (kante.x - anker.x))
            } else {
                nh = maxOf(minGroesse, z.sy * (kante.y - anker.y))
            }
            val cx = if (z.sx != 0) anker.x + z.sx * nb / 2f else 0f
            val cy = if (z.sy != 0) anker.y + z.sy * nh / 2f else 0f
            o.copy(
                mitteX = o.mitteX + cx * c - cy * s,
                mitteY = o.mitteY + cx * s + cy * c,
                breite = nb,
                hoehe = nh,
            )
        }
    }
}

/** Zeichnet einen Gegenstand gedreht an seine Stelle (Bildschirmkoordinaten). */
private fun DrawScope.zeichneGegenstandBild(
    g: Gegenstand,
    mitte: Offset,
    faktor: Float,
    dichte: Float,
    inDerHand: Boolean,
    umriss: Color?,
    umrissBreite: Float,
    pixelProMeter: Float,
) {
    val art = GegenstandsArt.vonSchluessel(g.art)
    val haupt = g.farbe?.let { Color(it) } ?: art.farbe
    val w = g.breite * faktor
    val h = g.hoehe * faktor
    withTransform({
        translate(mitte.x, mitte.y)
        rotate(g.drehung, Offset.Zero)
        translate(-w / 2f, -h / 2f)
    }) {
        zeichneGegenstand(art, w, h, if (inDerHand) lerp(haupt, Color.White, 0.35f) else haupt, dichte, pixelProMeter)
        if (inDerHand) drawRect(Color.White.copy(alpha = 0.2f), size = Size(w, h))
        if (umriss != null) drawRect(umriss, size = Size(w, h), style = Stroke(width = umrissBreite))
    }
}

/** Zeichnet ein freies Gebäude aus seinen Punkten (relativ zur Mitte), gedreht, mit Dachziegel-Muster. */
private fun DrawScope.zeichneGebaeude(
    g: Gegenstand,
    ps: List<Gegenstandspunkt>,
    mitte: Offset,
    faktor: Float,
    dichte: Float,
    inDerHand: Boolean,
    umriss: Color?,
    umrissBreite: Float,
    pixelProMeter: Float,
) {
    if (ps.size < 3) return
    val grund = g.farbe?.let { Color(it) } ?: GegenstandsArt.GEBAEUDE_FREI.farbe
    val haupt = if (inDerHand) lerp(grund, Color.White, 0.35f) else grund
    val rand = lerp(grund, Color.Black, 0.45f)
    withTransform({
        translate(mitte.x, mitte.y)
        rotate(g.drehung, Offset.Zero)
    }) {
        val bild = ps.map { Offset(it.x * faktor, it.y * faktor) }
        val pfad = kurvenPfad(bild, ps.map { it.rund }, geschlossen = true)
        drawPath(pfad, haupt)
        // Dachziegel: Reihen alle 0,5 m mit versetzten Fugen; zu kleine Muster werden weggelassen
        val reihe = 0.5f * pixelProMeter
        val minX = bild.minOf { it.x }
        val maxX = bild.maxOf { it.x }
        val minY = bild.minOf { it.y }
        val maxY = bild.maxOf { it.y }
        val ziegel = reihe * 1.6f
        if (reihe >= 5f * dichte && ((maxX - minX) / ziegel) * ((maxY - minY) / reihe) <= 4000f) {
            clipPath(pfad) {
                var y = minY
                var r = 0
                while (y < maxY) {
                    drawLine(rand.copy(alpha = 0.45f), Offset(minX, y), Offset(maxX, y), strokeWidth = dichte)
                    var x = minX + (if (r % 2 == 0) 0f else ziegel / 2f)
                    while (x < maxX) {
                        drawLine(rand.copy(alpha = 0.45f), Offset(x, y), Offset(x, y + reihe), strokeWidth = dichte)
                        x += ziegel
                    }
                    y += reihe
                    r++
                }
            }
        }
        if (inDerHand) drawPath(pfad, Color.White.copy(alpha = 0.2f))
        drawPath(pfad, rand, style = Stroke(width = 2f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (umriss != null) {
            drawPath(pfad, umriss, style = Stroke(width = umrissBreite, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * Geöffneter Garten: Zeichenfläche mit Zoomen und Verschieben (2b), Flächen
 * zeichnen (2c) und Flächen bearbeiten (2e).
 *
 * Koordinaten: Bildschirmposition = Skizzenpunkt * zoom * dichte + verschiebung.
 * Die Verschiebung wird in dp gespeichert, damit sie unabhängig von der Bildschirmdichte ist.
 *
 * Bedienung: In der Ansicht verschiebt ein Finger, zwei Finger zoomen. Im Bearbeitungsmodus
 * gehört ein Finger dem Zeichnen, Auswählen und Ziehen; verschoben und gezoomt wird dort
 * mit zwei Fingern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GartenScreen(
    garten: Garten,
    flaechen: List<Flaeche>,
    ebenen: List<Ebene>,
    gegenstaende: List<Gegenstand>,
    gegenstandspunkte: List<Gegenstandspunkt>,
    punkte: List<Punkt>,
    bearbeiten: Boolean,
    viewModel: GartenViewModel,
    onBearbeiten: () -> Unit,
    onFertig: () -> Unit,
    onZurueck: () -> Unit,
    onAnsichtGeaendert: (zoom: Float, x: Float, y: Float) -> Unit,
) {
    val dichte = LocalDensity.current.density
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val haptik = LocalHapticFeedback.current
    val zeichnung = viewModel.zeichnung
    val formAuswahl = viewModel.formAuswahl
    val einrasten = viewModel.einrasten
    val gartenId = garten.id

    // Ansicht pro Garten neu aus den gespeicherten Werten starten (nur beim Öffnen).
    var zoom by remember(gartenId) { mutableFloatStateOf(garten.ansichtZoom) }
    var verschiebungX by remember(gartenId) { mutableFloatStateOf(garten.ansichtX * dichte) }
    var verschiebungY by remember(gartenId) { mutableFloatStateOf(garten.ansichtY * dichte) }

    // Ansicht kurz nach der letzten Bewegung speichern (nicht bei jedem Pixel).
    LaunchedEffect(gartenId, zoom, verschiebungX, verschiebungY) {
        delay(600)
        onAnsichtGeaendert(zoom, verschiebungX / dichte, verschiebungY / dichte)
    }

    val punkteJeFlaeche = remember(punkte) { punkte.groupBy { it.flaecheId } }
    val gpJeGegenstand = remember(gegenstandspunkte) { gegenstandspunkte.groupBy { it.gegenstandId } }
    val gewaehlteFlaeche = flaechen.firstOrNull { it.id == viewModel.auswahlFlaeche }
    val gewaehltePunkte = gewaehlteFlaeche?.let { punkteJeFlaeche[it.id] }.orEmpty()
    val gewaehlterPunkt = gewaehltePunkte.firstOrNull { it.id == viewModel.auswahlPunkt }

    // Ebenen: gezeichnet wird von hinten nach vorn, ausgeblendete Ebenen werden übersprungen.
    val ebenenRang = remember(ebenen) { ebenen.withIndex().associate { it.value.id to it.index } }
    val verborgeneEbenen = remember(ebenen) { ebenen.filter { it.sicht == 2 }.map { it.id }.toSet() }
    val blasseEbenen = remember(ebenen) { ebenen.filter { it.sicht == 1 }.map { it.id }.toSet() }
    val geordneteFlaechen = remember(flaechen, ebenenRang) {
        flaechen.sortedWith(
            compareBy<Flaeche>({ ebenenRang[it.ebeneId] ?: -1 }, { it.reihenfolge }, { it.id }),
        )
    }
    val sichtbareFlaechen = remember(geordneteFlaechen, verborgeneEbenen) {
        geordneteFlaechen.filter { it.ebeneId !in verborgeneEbenen }
    }
    // Flächen und Gegenstände in einer gemeinsamen Reihenfolge: Ebene (hinten zuerst), dann Reihenfolge.
    val sichtbareElemente = remember(flaechen, gegenstaende, ebenenRang, verborgeneEbenen) {
        val alle = flaechen.map { Element(it.ebeneId, ebenenRang[it.ebeneId] ?: -1, it.reihenfolge, it.id, it, null) } +
            gegenstaende.map { Element(it.ebeneId, ebenenRang[it.ebeneId] ?: -1, it.reihenfolge, it.id, null, it) }
        alle.filter { it.ebeneId !in verborgeneEbenen }
            .sortedWith(compareBy<Element>({ it.rang }, { it.reihenfolge }, { it.gegenstand != null }, { it.id }))
    }
    val gewaehlterGegenstand = gegenstaende.firstOrNull { it.id == viewModel.auswahlGegenstand }
    val gewaehlterGebaeudePunkt = gewaehlterGegenstand?.let { g ->
        gpJeGegenstand[g.id]?.firstOrNull { it.id == viewModel.auswahlGegenstandPunkt }
    }

    // Aktive Ebene: die des ausgewählten Elements, sonst die zuletzt gewählte, sonst die vorderste.
    val aktiveEbeneId: Long? = gewaehlteFlaeche?.ebeneId
        ?: gewaehlterGegenstand?.ebeneId
        ?: viewModel.aktiveEbeneId?.takeIf { id -> ebenen.any { it.id == id } }
        ?: ebenen.lastOrNull()?.id
    LaunchedEffect(gewaehlteFlaeche?.id, gewaehlteFlaeche?.ebeneId) {
        gewaehlteFlaeche?.let { viewModel.setzeAktiveEbene(it.ebeneId) }
    }
    LaunchedEffect(gewaehlterGegenstand?.id, gewaehlterGegenstand?.ebeneId) {
        gewaehlterGegenstand?.let { viewModel.setzeAktiveEbene(it.ebeneId) }
    }

    // Zustand der Gesten
    var zeigerPos by remember { mutableStateOf<Offset?>(null) } // Finger beim Zeichnen/Aufziehen (Bildschirm)
    var formStart by remember { mutableStateOf<Offset?>(null) } // Startpunkt einer aufgezogenen Form (Skizze)
    var druckStart by remember { mutableStateOf<Offset?>(null) } // Fingerauflage im Auswahlmodus (Bildschirm)
    var bewegt by remember { mutableStateOf(false) }
    var ziehen by remember { mutableStateOf<Ziehen?>(null) }
    var ziehenFertig by remember { mutableStateOf(false) }
    var gZiehen by remember { mutableStateOf<GZiehen?>(null) } // laufendes Ziehen an einem Gegenstand
    var letzteVibration by remember { mutableFloatStateOf(-1f) }
    var platzOrt by remember { mutableStateOf<Offset?>(null) } // angetippte Stelle, für die der Katalog offen ist
    var gegenstandMenue by remember { mutableStateOf<Offset?>(null) }
    var gNamenDialog by remember { mutableStateOf(false) }
    var gLoeschenDialog by remember { mutableStateOf(false) }
    var infoGegenstandId by remember { mutableStateOf<Long?>(null) }

    // Dialoge
    var hilfeDialog by remember { mutableStateOf(false) }
    var formenDialog by remember { mutableStateOf(false) }
    var namenDialog by remember { mutableStateOf(false) }
    var loeschenDialog by remember { mutableStateOf(false) }
    // Flächenmenü (langer Tipp auf eine Fläche): Stelle des Fingers, sonst null
    var flaechenMenue by remember { mutableStateOf<Offset?>(null) }
    var oberflaecheDialog by remember { mutableStateOf(false) }
    var flaechenFarbeDialog by remember { mutableStateOf(false) }
    var gFarbeDialog by remember { mutableStateOf(false) }
    // In der Ansicht angetippte Fläche (zeigt Name und Oberfläche)
    var infoFlaecheId by remember { mutableStateOf<Long?>(null) }
    // Liste aller Ebenen (Auge in der Ebenenleiste)
    var ebenenOffen by remember { mutableStateOf(false) }
    var ebeneUmbenennenId by remember { mutableStateOf<Long?>(null) }
    var ebeneLoeschenId by remember { mutableStateOf<Long?>(null) }
    val linealPinsel = remember { android.graphics.Paint().apply { isAntiAlias = true } }
    val massPinsel = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.WHITE
        }
    }

    val faktor = zoom * dichte
    fun zuSkizze(bild: Offset) = Offset((bild.x - verschiebungX) / faktor, (bild.y - verschiebungY) / faktor)
    fun zuBildschirm(skizze: Offset) = Offset(skizze.x * faktor + verschiebungX, skizze.y * faktor + verschiebungY)

    // Reichweite der Ausrichtungshilfe: 12 dp auf dem Bildschirm, umgerechnet in Skizzeneinheiten.
    val einrastSchwelle = 12f / zoom
    val zeichnet = zeichnung != null || formAuswahl != null

    // Ziele zum Einrasten an anderen Flächen und Gegenständen; nur während einer Geste berechnet.
    val ausserFlaecheId = ziehen?.flaecheId
    val ausserGegenstandId = gZiehen?.original?.id
    val geste = zeichnung != null || formAuswahl != null || ziehen != null || gZiehen != null
    val fremd: FremdZiele? = remember(
        geste, einrasten, sichtbareElemente, punkteJeFlaeche, gpJeGegenstand, ausserFlaecheId, ausserGegenstandId,
    ) {
        if (!geste || !einrasten) {
            null
        } else {
            val ecken = ArrayList<Offset>()
            val kanten = ArrayList<Pair<Offset, Offset>>()
            for (el in sichtbareElemente) {
                val f = el.flaeche
                val g = el.gegenstand
                if (f != null) {
                    if (f.id == ausserFlaecheId) continue
                    val ihre = punkteJeFlaeche[f.id] ?: continue
                    if (ihre.size < 3) continue
                    val lagen = ihre.map { Offset(it.x, it.y) }
                    ecken.addAll(lagen)
                    val poly = kurvenPolylinie(lagen, ihre.map { it.rund }, true)
                    for (i in poly.indices) kanten.add(poly[i] to poly[(i + 1) % poly.size])
                } else if (g != null) {
                    if (g.id == ausserGegenstandId) continue
                    val ps = gpJeGegenstand[g.id].orEmpty()
                    ecken.addAll(gEckenSkizze(g, ps))
                    kanten.addAll(gKantenSkizze(g, ps))
                }
            }
            FremdZiele(ecken, kanten)
        }
    }
    fun fangFuer(g: Gegenstand): Fang? =
        if (fremd != null) Fang(fremd, einrastSchwelle, gEckenSkizze(g, gpJeGegenstand[g.id].orEmpty())) else null
    fun fangFremd(sk: Offset): Offset =
        if (einrasten) rastePunktMitFremd(sk, emptyList(), einrastSchwelle, false, fremd).punkt else sk

    // --- Vorschau des nächsten Punktes beim Zeichnen ---
    val vorschau: EinrastErgebnis? = run {
        val roh = zeigerPos
        if (zeichnung != null && formAuswahl == null && roh != null) {
            val sk = zuSkizze(roh)
            if (einrasten) rastePunktMitFremd(sk, zeichnung, einrastSchwelle, true, fremd) else EinrastErgebnis(sk, emptyList())
        } else {
            null
        }
    }
    // Liegt der Finger nah am ersten Punkt, schließt Loslassen die Fläche.
    val nahErstemPunkt: Boolean = run {
        val roh = zeigerPos
        val erster = zeichnung?.firstOrNull()
        zeichnung != null && zeichnung.size >= 3 && roh != null && erster != null &&
            (zuBildschirm(erster) - roh).getDistance() < SCHLIESS_RADIUS_DP * dichte
    }

    // --- Ergebnis eines laufenden Ziehens (Punkt mit Einrasten, oder Flächenverschiebung) ---
    val flaechenFang: BewegungsErgebnis? = run {
        val z = ziehen
        if (z == null || z.punktId != null || fremd == null || !bewegt) {
            null
        } else {
            val ecken = punkteJeFlaeche[z.flaecheId].orEmpty().map { Offset(it.x, it.y) }
            rasteBewegung(ecken, z.aktuell - z.start, fremd, einrastSchwelle)
        }
    }
    val ziehDelta: Offset = flaechenFang?.delta ?: ziehen?.let { it.aktuell - it.start } ?: Offset.Zero
    val ziehErgebnis: EinrastErgebnis? = run {
        val z = ziehen
        val punktId = z?.punktId
        if (z == null || punktId == null) {
            null
        } else {
            val ihre = punkteJeFlaeche[z.flaecheId].orEmpty()
            val index = ihre.indexOfFirst { it.id == punktId }
            if (index < 0) {
                null
            } else {
                val original = Offset(ihre[index].x, ihre[index].y)
                val roh = original + ziehDelta
                if (einrasten && ihre.size >= 3) {
                    val vor = ihre[(index - 1 + ihre.size) % ihre.size]
                    val nach = ihre[(index + 1) % ihre.size]
                    rastePunktMitFremd(roh, listOf(Offset(vor.x, vor.y), Offset(nach.x, nach.y)), einrastSchwelle, false, fremd)
                } else {
                    EinrastErgebnis(roh, emptyList())
                }
            }
        }
    }

    /** Lage eines Punktes in Skizzenkoordinaten, mit dem laufenden Ziehen eingerechnet. */
    fun lage(flaeche: Flaeche, p: Punkt): Offset {
        val z = ziehen
        if (z != null && z.flaecheId == flaeche.id) {
            if (z.punktId == null) return Offset(p.x, p.y) + ziehDelta
            if (z.punktId == p.id && ziehErgebnis != null) return ziehErgebnis.punkt
        }
        return Offset(p.x, p.y)
    }

    /** Was liegt unter dem Finger bei der ausgewählten Fläche: ein Griff (Punkt) oder ihr Inneren? */
    fun griffBei(pos: Offset): Punkt? {
        val fl = gewaehlteFlaeche ?: return null
        var beste: Punkt? = null
        var besterAbstand = GRIFF_RADIUS_DP * dichte
        for (p in gewaehltePunkte) {
            val d = (zuBildschirm(lage(fl, p)) - pos).getDistance()
            if (d < besterAbstand) {
                beste = p
                besterAbstand = d
            }
        }
        return beste
    }

    fun bildPunkte(fl: Flaeche, liste: List<Punkt>): List<Offset> = liste.map { zuBildschirm(lage(fl, it)) }

    fun innerhalbGewaehlter(pos: Offset): Boolean {
        val fl = gewaehlteFlaeche ?: return false
        if (gewaehltePunkte.size < 3) return false
        val poly = kurvenPolylinie(bildPunkte(fl, gewaehltePunkte), gewaehltePunkte.map { it.rund }, true)
        return liegtInnen(poly, pos)
    }

    // --- Gegenstände: Lage, Griffe, Treffer ---
    val minGegenstand = 0.1f * garten.massstab

    /** Der Gegenstand mit dem laufenden Ziehen eingerechnet. */
    fun gLive(g: Gegenstand): Gegenstand {
        val z = gZiehen
        return if (z != null && z.original.id == g.id) {
            angepasst(z, einrasten, minGegenstand, if (bewegt) fangFuer(z.original) else null)
        } else {
            g
        }
    }

    /** Ein Punkt im Koordinatensystem des Gegenstands (Mitte = 0, vor der Drehung, Bildschirmpixel) auf dem Bildschirm. */
    fun gBild(g: Gegenstand, lx: Float, ly: Float): Offset {
        val c = zuBildschirm(Offset(g.mitteX, g.mitteY))
        val w = Math.toRadians(g.drehung.toDouble())
        val cs = cos(w).toFloat()
        val sn = sin(w).toFloat()
        return Offset(c.x + lx * cs - ly * sn, c.y + lx * sn + ly * cs)
    }

    /** Punkte eines freien Gebäudes mit dem laufenden Ziehen eines Punktes eingerechnet. */
    fun gPunkteLive(g: Gegenstand): List<Gegenstandspunkt> {
        val liste = gpJeGegenstand[g.id].orEmpty()
        val z = gZiehen
        val punktId = z?.punktId
        if (z == null || punktId == null || z.modus != GModus.PUNKT || z.original.id != g.id) return liste
        val ziel = gebaeudePunktZiel(z.original, liste, z, if (bewegt) fangFuer(z.original) else null, einrasten).first
        return liste.map { if (it.id == punktId) it.copy(x = ziel.x, y = ziel.y) else it }
    }

    fun gGriffe(g: Gegenstand): List<GriffInfo> {
        val w = g.breite * faktor
        val h = g.hoehe * faktor
        val eckRadius = (minOf(w, h) * 0.35f).coerceIn(14f * dichte, 26f * dichte)
        val liste = mutableListOf<GriffInfo>()
        if (istGebaeude(g)) {
            // Freies Gebäude: nur der Drehgriff (die Form ändern die Punkte)
            val minY = gPunkteLive(g).minOfOrNull { it.y * faktor } ?: (-h / 2f)
            liste.add(GriffInfo(0, 0, true, gBild(g, 0f, minY - 36f * dichte), 24f * dichte, gBild(g, 0f, minY)))
            return liste
        }
        if (minOf(w, h) < 40f * dichte) {
            // Schmaler Gegenstand (Zaun, Hecke, ...): nur Griffe an den Enden der langen Seite, damit die
            // Mitte zum Verschieben frei bleibt. Die Dicke ändert man nach dem Hineinzoomen.
            val laenge = maxOf(w, h)
            val rad = (laenge * 0.3f).coerceIn(12f * dichte, 22f * dichte)
            if (w >= h) {
                liste.add(GriffInfo(-1, 0, false, gBild(g, -w / 2f, 0f), rad))
                liste.add(GriffInfo(1, 0, false, gBild(g, w / 2f, 0f), rad))
                // Dicke: Griff an einem Stiel unterhalb (gegenüber vom Drehgriff)
                liste.add(GriffInfo(0, 1, false, gBild(g, 0f, h / 2f + 30f * dichte), 20f * dichte, gBild(g, 0f, h / 2f)))
            } else {
                liste.add(GriffInfo(0, -1, false, gBild(g, 0f, -h / 2f), rad))
                liste.add(GriffInfo(0, 1, false, gBild(g, 0f, h / 2f), rad))
                liste.add(GriffInfo(1, 0, false, gBild(g, w / 2f + 30f * dichte, 0f), 20f * dichte, gBild(g, w / 2f, 0f)))
            }
            liste.add(GriffInfo(0, 0, true, gBild(g, 0f, -(h / 2f + 36f * dichte)), 24f * dichte, gBild(g, 0f, -h / 2f)))
            return liste
        }
        for (sx in -1..1) {
            for (sy in -1..1) {
                if (sx == 0 && sy == 0) continue
                val ecke = sx != 0 && sy != 0
                // Kantengriffe nur, wenn die Kante lang genug ist (sonst liegen sie auf den Eckgriffen).
                if (!ecke && (if (sx == 0) w else h) < 64f * dichte) continue
                liste.add(GriffInfo(sx, sy, false, gBild(g, sx * w / 2f, sy * h / 2f), if (ecke) eckRadius else 20f * dichte))
            }
        }
        liste.add(GriffInfo(0, 0, true, gBild(g, 0f, -(h / 2f + 36f * dichte)), 24f * dichte, gBild(g, 0f, -h / 2f)))
        return liste
    }

    fun gGriffBei(g: Gegenstand, pos: Offset): GriffInfo? {
        var beste: GriffInfo? = null
        var besterAbstand = Float.MAX_VALUE
        for (gr in gGriffe(g)) {
            val d = (gr.pos - pos).getDistance()
            if (d < gr.radius && d < besterAbstand) {
                beste = gr
                besterAbstand = d
            }
        }
        return beste
    }

    fun gTrifft(g: Gegenstand, pos: Offset): Boolean {
        val c = zuBildschirm(Offset(g.mitteX, g.mitteY))
        val w = Math.toRadians(g.drehung.toDouble())
        val cs = cos(w).toFloat()
        val sn = sin(w).toFloat()
        val dx = pos.x - c.x
        val dy = pos.y - c.y
        val lx = dx * cs + dy * sn
        val ly = -dx * sn + dy * cs
        if (istGebaeude(g)) {
            val ps = gPunkteLive(g)
            return ps.size >= 3 &&
                liegtInnen(
                    kurvenPolylinie(ps.map { Offset(it.x * faktor, it.y * faktor) }, ps.map { it.rund }, true),
                    Offset(lx, ly),
                )
        }
        val halbB = maxOf(g.breite * faktor / 2f, 12f * dichte)
        val halbH = maxOf(g.hoehe * faktor / 2f, 12f * dichte)
        return abs(lx) <= halbB && abs(ly) <= halbH
    }

    /** Der Gebäudepunkt unter dem Finger (nur bei freien Gebäuden). */
    fun gPunktBei(g: Gegenstand, pos: Offset): Gegenstandspunkt? {
        var beste: Gegenstandspunkt? = null
        var besterAbstand = GRIFF_RADIUS_DP * dichte
        for (p in gPunkteLive(g)) {
            val d = (gBild(g, p.x * faktor, p.y * faktor) - pos).getDistance()
            if (d < besterAbstand) {
                beste = p
                besterAbstand = d
            }
        }
        return beste
    }

    /** Eine Bildschirmposition im Koordinatensystem des Gegenstands (Skizzeneinheiten, vor der Drehung). */
    fun gLokalSkizze(g: Gegenstand, bild: Offset): Offset {
        val c = zuBildschirm(Offset(g.mitteX, g.mitteY))
        val w = Math.toRadians(g.drehung.toDouble())
        val cs = cos(w).toFloat()
        val sn = sin(w).toFloat()
        val dx = bild.x - c.x
        val dy = bild.y - c.y
        return Offset((dx * cs + dy * sn) / faktor, (-dx * sn + dy * cs) / faktor)
    }

    /** Das oberste Element (Fläche oder Gegenstand) unter dem Finger. */
    fun elementBei(pos: Offset): Element? {
        for (el in sichtbareElemente.asReversed()) {
            val f = el.flaeche
            val g = el.gegenstand
            if (f != null) {
                val ihre = punkteJeFlaeche[f.id] ?: continue
                if (ihre.size < 3) continue
                val poly = kurvenPolylinie(bildPunkte(f, ihre), ihre.map { it.rund }, true)
                if (liegtInnen(poly, pos)) return el
            } else if (g != null && gTrifft(g, pos)) {
                return el
            }
        }
        return null
    }

    /** Die oberste Fläche unter dem Finger (oder null). */
    fun flaecheBei(pos: Offset): Flaeche? {
        for (kandidat in sichtbareFlaechen.asReversed()) {
            val ihre = punkteJeFlaeche[kandidat.id] ?: continue
            if (ihre.size < 3) continue
            val poly = kurvenPolylinie(bildPunkte(kandidat, ihre), ihre.map { it.rund }, true)
            if (liegtInnen(poly, pos)) return kandidat
        }
        return null
    }

    // --- Tipp im Auswahlmodus ---
    fun beiTippAuswahl(pos: Offset) {
        val gg = gewaehlterGegenstand
        if (gg != null) {
            if (istGebaeude(gg)) {
                val pg = gPunktBei(gg, pos)
                if (pg != null) {
                    viewModel.waehleGegenstandPunkt(gg.id, pg.id)
                    return
                }
                if (gGriffBei(gg, pos) != null) return
                val ps = gPunkteLive(gg)
                if (ps.size >= 3) {
                    val treffer = naechsterKurvenPunkt(
                        ps.map { gBild(gg, it.x * faktor, it.y * faktor) },
                        ps.map { it.rund },
                        true,
                        pos,
                    )
                    if (treffer != null && treffer.abstand < KANTE_RADIUS_DP * dichte) {
                        // Tipp auf den Rand: neuer Punkt
                        val neu = gLokalSkizze(gg, treffer.punkt)
                        viewModel.fuegeGebaeudePunktEin(gartenId, gg.id, treffer.segment + 1, neu.x, neu.y, ps[treffer.segment].rund)
                        return
                    }
                }
                if (gTrifft(gg, pos)) {
                    viewModel.waehleGegenstandPunkt(gg.id, null) // Tipp ins Innere: nur den Punkt abwählen
                    return
                }
            } else if (gGriffBei(gg, pos) != null || gTrifft(gg, pos)) {
                return // Tipp auf den eigenen Gegenstand
            }
        }
        val fl = gewaehlteFlaeche
        if (fl != null) {
            // 1. Griff antippen: Punkt auswählen
            val griff = griffBei(pos)
            if (griff != null) {
                viewModel.waehlePunkt(fl.id, griff.id)
                return
            }
            // 2. Rand antippen: neuen Punkt einfügen
            if (gewaehltePunkte.size >= 3) {
                val treffer = naechsterKurvenPunkt(
                    bildPunkte(fl, gewaehltePunkte),
                    gewaehltePunkte.map { it.rund },
                    true,
                    pos,
                )
                if (treffer != null && treffer.abstand < KANTE_RADIUS_DP * dichte) {
                    val neu = zuSkizze(treffer.punkt)
                    viewModel.fuegePunktEin(
                        gartenId,
                        fl.id,
                        treffer.segment + 1,
                        neu.x,
                        neu.y,
                        gewaehltePunkte[treffer.segment].rund,
                    )
                    return
                }
            }
        }
        // 3. Das oberste Element (Fläche oder Gegenstand) unter dem Finger auswählen, sonst Auswahl aufheben
        val treffer = elementBei(pos)
        val trefferFlaeche = treffer?.flaeche
        val trefferGegenstand = treffer?.gegenstand
        if (trefferFlaeche != null) {
            viewModel.waehleFlaeche(trefferFlaeche.id)
        } else if (trefferGegenstand != null) {
            viewModel.waehleGegenstand(trefferGegenstand.id)
        } else {
            viewModel.waehleFlaeche(null)
        }
    }

    val beiZeiger = rememberUpdatedState<(Offset?) -> Unit> { pos ->
        if (pos == null) {
            // Geste abgebrochen oder beendet
            zeigerPos = null
            formStart = null
            druckStart = null
            if (!ziehenFertig) {
                ziehen = null
                gZiehen = null
            }
        } else {
            zeigerPos = pos
            if (formAuswahl != null) {
                if (formStart == null) formStart = fangFremd(zuSkizze(pos))
            } else if (zeichnung == null && bearbeiten && !viewModel.platzieren) {
                val start = druckStart
                if (start == null) {
                    // Finger ist gerade aufgesetzt worden: was liegt darunter?
                    druckStart = pos
                    bewegt = false
                    val gg = gewaehlterGegenstand
                    if (gg != null) {
                        // Ausgewählter Gegenstand: Griff (Größe, Drehen) oder Gegenstand selbst (Verschieben)?
                        ziehen = null
                        val sk = zuSkizze(pos)
                        val gr = gGriffBei(gg, pos)
                        val gp = if (istGebaeude(gg)) gPunktBei(gg, pos) else null
                        gZiehen = if (gp != null) {
                            GZiehen(gg, GModus.PUNKT, 0, 0, sk, sk, gp.id)
                        } else if (gr != null) {
                            GZiehen(gg, if (gr.drehen) GModus.DREHEN else GModus.GROESSE, gr.sx, gr.sy, sk, sk)
                        } else if (gTrifft(gg, pos)) {
                            GZiehen(gg, GModus.VERSCHIEBEN, 0, 0, sk, sk)
                        } else {
                            null
                        }
                    } else {
                        gZiehen = null
                        val fl = gewaehlteFlaeche
                        val griff = griffBei(pos)
                        ziehen = if (fl != null && griff != null) {
                            Ziehen(fl.id, griff.id, zuSkizze(pos), zuSkizze(pos))
                        } else if (fl != null && innerhalbGewaehlter(pos)) {
                            Ziehen(fl.id, null, zuSkizze(pos), zuSkizze(pos))
                        } else {
                            null
                        }
                    }
                } else {
                    if (!bewegt && (pos - start).getDistance() > touchSlop) {
                        bewegt = true
                        // Fläche "in der Hand": kurzes Fühlen, dazu Schatten und Rahmen in der Anzeige
                        if ((ziehen != null && ziehen?.punktId == null) || gZiehen?.modus == GModus.VERSCHIEBEN) {
                            haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    val z = ziehen
                    if (bewegt && z != null) ziehen = z.copy(aktuell = zuSkizze(pos))
                    val gz = gZiehen
                    if (bewegt && gz != null) {
                        val neu = gz.copy(aktuell = zuSkizze(pos))
                        gZiehen = neu
                        if (neu.modus == GModus.DREHEN && einrasten) {
                            // Kurze Vibration, wenn der Gegenstand auf 0, 90, 180 oder 270 Grad einrastet.
                            val winkel = angepasst(neu, true, minGegenstand).drehung
                            if (winkel % 90f == 0f) {
                                if (winkel != letzteVibration) {
                                    haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                                    letzteVibration = winkel
                                }
                            } else {
                                letzteVibration = -1f
                            }
                        }
                    }
                }
            }
        }
    }

    val beiTippAnsicht = rememberUpdatedState<(Offset) -> Unit> { pos ->
        val treffer = if (bearbeiten) null else elementBei(pos)
        infoFlaecheId = treffer?.flaeche?.id
        infoGegenstandId = treffer?.gegenstand?.id
    }

    val beiLangemDruck = rememberUpdatedState<(Offset) -> Unit> { pos ->
        if (bearbeiten && zeichnung == null && formAuswahl == null && !viewModel.platzieren) {
            val treffer = elementBei(pos)
            val trefferFlaeche = treffer?.flaeche
            val trefferGegenstand = treffer?.gegenstand
            if (trefferFlaeche != null) {
                ziehen = null
                gZiehen = null
                viewModel.waehleFlaeche(trefferFlaeche.id)
                haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                flaechenMenue = pos
            } else if (trefferGegenstand != null) {
                ziehen = null
                gZiehen = null
                viewModel.waehleGegenstand(trefferGegenstand.id)
                haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                gegenstandMenue = pos
            }
        }
    }

    val beiLoslassen = rememberUpdatedState<(Offset) -> Unit> { pos ->
        if (formAuswahl != null) {
            val start = formStart
            if (start != null) {
                val punkteDerForm = formPunkte(formAuswahl, start, fangFremd(zuSkizze(pos)), MIN_FORM_DP / faktor)
                if (punkteDerForm.isNotEmpty()) {
                    viewModel.legeFormAn(gartenId, punkteDerForm.map { it.lage }, punkteDerForm.map { it.rund })
                }
            }
        } else if (zeichnung != null) {
            if (nahErstemPunkt) {
                viewModel.schliesseFlaecheAb(gartenId)
            } else {
                val sk = zuSkizze(pos)
                viewModel.setzePunkt(if (einrasten) rastePunktMitFremd(sk, zeichnung, einrastSchwelle, true, fremd).punkt else sk)
            }
        } else if (viewModel.platzieren) {
            platzOrt = zuSkizze(pos)
        } else if (bearbeiten) {
            val z = ziehen
            val gz = gZiehen
            if (gz != null && bewegt) {
                val fertig = gz.copy(aktuell = zuSkizze(pos))
                val ziehPunkt = fertig.punktId
                if (fertig.modus == GModus.PUNKT && ziehPunkt != null) {
                    val liste = gpJeGegenstand[fertig.original.id].orEmpty()
                    val p = liste.firstOrNull { it.id == ziehPunkt }
                    if (p != null) {
                        val ziel = gebaeudePunktZiel(fertig.original, liste, fertig, fangFuer(fertig.original), einrasten).first
                        viewModel.verschiebeGebaeudePunkt(gartenId, p.id, ziel.x, ziel.y)
                    }
                } else {
                    viewModel.setzeGegenstandLage(gartenId, angepasst(fertig, einrasten, minGegenstand, fangFuer(fertig.original)))
                }
                gZiehen = fertig
                ziehenFertig = true
            } else if (z != null && bewegt) {
                val fertig = z.copy(aktuell = zuSkizze(pos))
                if (fertig.punktId != null) {
                    val ergebnis = ziehErgebnis
                    if (ergebnis != null) {
                        viewModel.verschiebePunkt(gartenId, fertig.punktId, ergebnis.punkt.x, ergebnis.punkt.y)
                    }
                } else {
                    var delta = fertig.aktuell - fertig.start
                    if (fremd != null) {
                        val ecken = punkteJeFlaeche[fertig.flaecheId].orEmpty().map { Offset(it.x, it.y) }
                        delta = rasteBewegung(ecken, delta, fremd, einrastSchwelle).delta
                    }
                    viewModel.verschiebeFlaeche(gartenId, fertig.flaecheId, delta.x, delta.y)
                }
                // Die Anzeige bleibt kurz auf dem neuen Stand, bis die Datenbank nachgezogen hat.
                ziehen = fertig
                ziehenFertig = true
            } else if (!bewegt) {
                ziehen = null
                beiTippAuswahl(druckStart ?: pos)
            }
        }
    }

    // Nach dem Loslassen: Anzeige wieder auf die Daten aus der Datenbank umstellen.
    LaunchedEffect(ziehenFertig, punkte, gegenstaende) {
        if (ziehenFertig) {
            delay(150)
            ziehen = null
            gZiehen = null
            ziehenFertig = false
        }
    }
    // Beim Wechsel der Betriebsart keine Reste einer Geste behalten.
    LaunchedEffect(bearbeiten, zeichnet) {
        zeigerPos = null
        formStart = null
        druckStart = null
        ziehen = null
        gZiehen = null
        ziehenFertig = false
        infoFlaecheId = null
        infoGegenstandId = null
    }

    // Stellen, an denen gerade an einer fremden Ecke oder Kante eingerastet wird (orange Markierung)
    val fangMarken: List<Offset> = run {
        val m = ArrayList<Offset>()
        if (zeichnung != null) vorschau?.marke?.let { m.add(it) }
        val zf = ziehen
        if (zf != null && bewegt) {
            if (zf.punktId == null) m.addAll(rasteMarken(flaechenFang)) else ziehErgebnis?.marke?.let { m.add(it) }
        }
        val gz = gZiehen
        if (gz != null && bewegt && einrasten) {
            if (gz.modus == GModus.PUNKT) {
                val liste = gpJeGegenstand[gz.original.id].orEmpty()
                gebaeudePunktZiel(gz.original, liste, gz, fangFuer(gz.original), true).second?.marke?.let { m.add(it) }
            } else if (gz.modus != GModus.DREHEN) {
                angepasst(gz, true, minGegenstand, fangFuer(gz.original), m)
            }
        }
        val fe = zeigerPos
        if (formAuswahl != null && formStart != null && fe != null && einrasten) {
            rastePunktMitFremd(zuSkizze(fe), emptyList(), einrastSchwelle, false, fremd).marke?.let { m.add(it) }
        }
        m
    }

    // Lupe: Stelle (Bildschirm), die vergrößert gezeigt wird, sonst null
    val lupeFokus: Offset? = run {
        val finger = zeigerPos
        val zf = ziehen
        val gz = gZiehen
        if (finger == null) {
            null
        } else if (zeichnung != null) {
            vorschau?.let { zuBildschirm(it.punkt) } ?: finger
        } else if (formAuswahl != null) {
            if (formStart != null) finger else null
        } else if (bewegt && zf != null) {
            val e = ziehErgebnis
            if (zf.punktId != null && e != null) zuBildschirm(e.punkt) else finger
        } else if (bewegt && gz != null && gz.modus != GModus.DREHEN) {
            val punktId = gz.punktId
            val live = gLive(gz.original)
            val pg = if (gz.modus == GModus.PUNKT && punktId != null) gPunkteLive(live).firstOrNull { it.id == punktId } else null
            if (pg != null) gBild(live, pg.x * faktor, pg.y * faktor) else finger
        } else {
            null
        }
    }

    // Maß oder Winkel, der beim Ändern von Größe oder Drehung neben dem Finger steht
    val ziehGgl = gZiehen
    val liveText: String? = if (ziehGgl != null && bewegt && ziehGgl.modus != GModus.VERSCHIEBEN) {
        val live = gLive(ziehGgl.original)
        if (ziehGgl.modus == GModus.GROESSE) {
            stringResource(
                R.string.masse_live,
                zahlText(live.breite / garten.massstab),
                zahlText(live.hoehe / garten.massstab),
            )
        } else {
            stringResource(R.string.grad_live, live.drehung.roundToInt().toString())
        }
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (bearbeiten) {
                            stringResource(R.string.bearbeiten_titel, garten.name)
                        } else {
                            garten.name
                        },
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (bearbeiten) onFertig else onZurueck) {
                        Icon(
                            painter = painterResource(R.drawable.ic_zurueck),
                            contentDescription = stringResource(R.string.zurueck),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { hilfeDialog = true }) {
                        Text(stringResource(R.string.hilfe_knopf), style = MaterialTheme.typography.titleLarge)
                    }
                    if (bearbeiten) {
                        TextButton(onClick = onFertig) { Text(stringResource(R.string.fertig)) }
                    } else {
                        IconButton(onClick = onBearbeiten) {
                            Icon(
                                painter = painterResource(R.drawable.ic_spaten),
                                contentDescription = stringResource(R.string.bearbeiten),
                            )
                        }
                    }
                },
                colors = if (bearbeiten) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )
        },
        bottomBar = {
            if (bearbeiten) {
                Werkzeugleiste(
                    zeichnung = zeichnung,
                    zeichnungRund = viewModel.zeichnungRund,
                    formAuswahl = formAuswahl,
                    einrasten = einrasten,
                    kannRueckgaengig = viewModel.anzahlRueckgaengig > 0,
                    gewaehlteFlaeche = gewaehlteFlaeche,
                    gegenstandGewaehlt = gewaehlterGegenstand != null,
                    gebaeudePunkt = gewaehlterGebaeudePunkt,
                    gebaeudePunktLoeschenMoeglich = (gewaehlterGegenstand?.let { gpJeGegenstand[it.id]?.size } ?: 0) > 3,
                    onGebaeudePunktLoeschen = {
                        val g = gewaehlterGegenstand
                        val pu = gewaehlterGebaeudePunkt
                        if (g != null && pu != null) viewModel.loescheGebaeudePunkt(gartenId, g.id, pu.id, pu.nr)
                    },
                    onGebaeudePunktRund = {
                        val pu = gewaehlterGebaeudePunkt
                        if (pu != null) viewModel.setzeGebaeudePunktRund(gartenId, pu.id, !pu.rund)
                    },
                    platzieren = viewModel.platzieren,
                    gewaehlterPunkt = gewaehlterPunkt,
                    punktLoeschenMoeglich = gewaehltePunkte.size > 3,
                    onFlaecheZeichnen = viewModel::starteZeichnung,
                    onFormenWaehlen = { formenDialog = true },
                    onGegenstand = viewModel::starteGegenstandPlatzieren,
                    onPlatzierenAbbrechen = viewModel::brichPlatzierenAb,
                    onRueckgaengig = {
                        if (zeichnung != null) viewModel.entferneLetztenPunkt() else viewModel.macheRueckgaengig(gartenId)
                    },
                    onAbbrechen = {
                        if (formAuswahl != null) viewModel.brichFormAb() else viewModel.brichZeichnungAb()
                    },
                    onSchalteRund = viewModel::schalteRund,
                    onSchalteEinrasten = viewModel::schalteEinrasten,
                    onAbwaehlen = { viewModel.waehleFlaeche(null) },
                    onPunktLoeschen = {
                        val fl = gewaehlteFlaeche
                        val pu = gewaehlterPunkt
                        if (fl != null && pu != null) viewModel.loeschePunkt(gartenId, fl.id, pu.id, pu.nr)
                    },
                    onPunktRundSchalten = {
                        val pu = gewaehlterPunkt
                        if (pu != null) viewModel.setzePunktRund(gartenId, pu.id, !pu.rund)
                    },
                )
            }
        },
    ) { innerPadding ->
        val hintergrund = MaterialTheme.colorScheme.surface
        val gitterFarbe = MaterialTheme.colorScheme.outlineVariant
        val achsenFarbe = MaterialTheme.colorScheme.primary
        val rahmenFarbe = MaterialTheme.colorScheme.tertiary
        val linealHintergrund = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        val linealLinie = MaterialTheme.colorScheme.outline
        val linealText = MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(hintergrund)
                .then(
                    if (bearbeiten) Modifier.border(4.dp, rahmenFarbe) else Modifier,
                ),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bearbeiten) {
                        gartenGesten(
                            einFingerVerschiebt = !bearbeiten,
                            einFingerAktion = bearbeiten,
                            beiZeiger = { beiZeiger.value(it) },
                            beiLoslassen = { beiLoslassen.value(it) },
                            beiLangemDruck = { beiLangemDruck.value(it) },
                            beiTipp = { beiTippAnsicht.value(it) },
                            beiTransform = { schwerpunkt, verschieben, zoomFaktor ->
                                val neuerZoom = (zoom * zoomFaktor).coerceIn(ZOOM_MIN, ZOOM_MAX)
                                val echterFaktor = neuerZoom / zoom
                                // Zoom um den Punkt zwischen den Fingern, damit der Inhalt dort stehen bleibt.
                                verschiebungX = schwerpunkt.x - (schwerpunkt.x - verschiebungX) * echterFaktor + verschieben.x
                                verschiebungY = schwerpunkt.y - (schwerpunkt.y - verschiebungY) * echterFaktor + verschieben.y
                                zoom = neuerZoom
                            },
                        )
                    },
            ) {
              // Die ganze Szene (Gitter, Flächen, Gegenstände, Griffe, Hilfslinien); die Lupe zeichnet sie ein zweites Mal.
              fun zeichneSzene() {
                zeichneGitter(zoom, dichte, garten.massstab, Offset(verschiebungX, verschiebungY), gitterFarbe, achsenFarbe)

                // Fertige Flächen: die mit der höheren Reihenfolge liegen oben (sind schon sortiert).
                fun zeichneElement(el: Element) {
                    val gegenstand = el.gegenstand
                    if (gegenstand != null) {
                        val live = gLive(gegenstand)
                        val blassG = live.ebeneId in blasseEbenen
                        if (blassG) drawContext.canvas.saveLayer(SZENE_GRENZE, Paint().apply { alpha = 0.35f })
                        val inDerHandG = bewegt && gZiehen?.original?.id == gegenstand.id &&
                            gZiehen?.modus == GModus.VERSCHIEBEN
                        val imInfo = !bearbeiten && gegenstand.id == infoGegenstandId
                        val umrissFarbe = if (imInfo) achsenFarbe else if (inDerHandG) rahmenFarbe else null
                        val umrissDicke = (if (imInfo) 4f else 6f) * dichte
                        if (istGebaeude(live)) {
                            zeichneGebaeude(
                                live,
                                gPunkteLive(live),
                                zuBildschirm(Offset(live.mitteX, live.mitteY)),
                                faktor,
                                dichte,
                                inDerHandG,
                                umrissFarbe,
                                umrissDicke,
                                faktor * garten.massstab,
                            )
                        } else {
                            zeichneGegenstandBild(
                                live,
                                zuBildschirm(Offset(live.mitteX, live.mitteY)),
                                faktor,
                                dichte,
                                inDerHandG,
                                umrissFarbe,
                                umrissDicke,
                                faktor * garten.massstab,
                            )
                        }
                        if (blassG) drawContext.canvas.restore()
                        return
                    }
                    val flaeche = el.flaeche ?: return
                    val ihre = punkteJeFlaeche[flaeche.id] ?: return
                    if (ihre.size < 3) return
                    val bild = bildPunkte(flaeche, ihre)
                    val pfad = kurvenPfad(bild, ihre.map { it.rund }, geschlossen = true)
                    val blass = flaeche.ebeneId in blasseEbenen
                    if (blass) drawContext.canvas.saveLayer(SZENE_GRENZE, Paint().apply { alpha = 0.35f })
                    val inDerHand = bewegt && ziehen?.punktId == null && ziehen?.flaecheId == flaeche.id
                    val art = Oberflaeche.vonSchluessel(flaeche.oberflaeche)
                    // Angefasste Fläche: Farbe heller und greller, damit man sieht, was man in der Hand hat
                    val aus = art.aussehen(flaeche.farbe)
                    drawPath(pfad, if (inDerHand) lerp(aus.fuellung, Color.White, 0.45f) else aus.fuellung)
                    zeichneMuster(art, pfad, bild, faktor, dichte, aus.muster)
                    if (inDerHand) drawPath(pfad, Color.White.copy(alpha = 0.2f))
                    drawPath(
                        pfad,
                        aus.rand,
                        style = Stroke(width = 2f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    if (!bearbeiten && flaeche.id == infoFlaecheId) {
                        drawPath(
                            pfad,
                            achsenFarbe,
                            style = Stroke(width = 4f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                    if (bearbeiten && flaeche.id == gewaehlteFlaeche?.id) {
                        drawPath(
                            pfad,
                            rahmenFarbe,
                            style = Stroke(
                                width = (if (inDerHand) 6f else 3f) * dichte,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                    if (blass) drawContext.canvas.restore()
                }

                // Fertige Flächen und Gegenstände: die mit der höheren Reihenfolge liegen oben (sind schon sortiert).
                for (el in sichtbareElemente) zeichneElement(el)

                // "Verdecktes zeigen": Jede Ebene wird noch einmal halbtransparent obenauf gezeichnet (von vorn nach
                // hinten, damit hintere Ebenen oben liegen). Innerhalb der Ebene bleibt die Reihenfolge, deshalb
                // scheint nur durch, was von einer weiter vorn liegenden Ebene verdeckt wird.
                if (viewModel.verdecktZeigen && ebenen.size > 1) {
                    val jeEbeneElemente = sichtbareElemente.groupBy { it.ebeneId }
                    for (e in ebenen.asReversed().drop(1)) {
                        val liste = jeEbeneElemente[e.id] ?: continue
                        drawContext.canvas.saveLayer(SZENE_GRENZE, Paint().apply { alpha = 0.35f })
                        for (el in liste) zeichneElement(el)
                        drawContext.canvas.restore()
                    }
                }

                // Griffe der ausgewählten Fläche: runde Punkte als Kreis, Ecken als Quadrat
                if (bearbeiten && gewaehlteFlaeche != null && zeichnung == null && formAuswahl == null) {
                    for (p in gewaehltePunkte) {
                        val mitte = zuBildschirm(lage(gewaehlteFlaeche, p))
                        val gewaehlt = p.id == gewaehlterPunkt?.id
                        val r = (if (gewaehlt) 13f else 10f) * dichte
                        if (p.rund) {
                            drawCircle(hintergrund, radius = r, center = mitte)
                            drawCircle(
                                rahmenFarbe,
                                radius = r,
                                center = mitte,
                                style = Stroke(width = 2.5f * dichte),
                            )
                            if (gewaehlt) drawCircle(rahmenFarbe, radius = r * 0.55f, center = mitte)
                        } else {
                            val ecke = Offset(mitte.x - r, mitte.y - r)
                            drawRect(hintergrund, topLeft = ecke, size = Size(2f * r, 2f * r))
                            drawRect(
                                rahmenFarbe,
                                topLeft = ecke,
                                size = Size(2f * r, 2f * r),
                                style = Stroke(width = 2.5f * dichte),
                            )
                            if (gewaehlt) {
                                drawRect(
                                    rahmenFarbe,
                                    topLeft = Offset(mitte.x - r * 0.55f, mitte.y - r * 0.55f),
                                    size = Size(1.1f * r, 1.1f * r),
                                )
                            }
                        }
                    }
                    // Hilfslinien beim Ziehen eines Punktes
                    val gestrichelt = PathEffect.dashPathEffect(floatArrayOf(12f * dichte, 8f * dichte))
                    for ((von, bis) in ziehErgebnis?.hilfslinien.orEmpty()) {
                        drawLine(
                            rahmenFarbe.copy(alpha = 0.8f),
                            zuBildschirm(von),
                            zuBildschirm(bis),
                            strokeWidth = 2f * dichte,
                            pathEffect = gestrichelt,
                        )
                    }
                }

                // Rahmen und Griffe des ausgewählten Gegenstands (immer ganz oben)
                val ausgewaehlt = gewaehlterGegenstand?.let { gLive(it) }
                if (bearbeiten && ausgewaehlt != null && zeichnung == null && formAuswahl == null && !viewModel.platzieren) {
                    if (istGebaeude(ausgewaehlt)) {
                        // Freies Gebäude: Umriss und Punktgriffe wie bei einer Fläche
                        val ps = gPunkteLive(ausgewaehlt)
                        if (ps.size >= 3) {
                            val bildPs = ps.map { gBild(ausgewaehlt, it.x * faktor, it.y * faktor) }
                            drawPath(
                                kurvenPfad(bildPs, ps.map { it.rund }, geschlossen = true),
                                rahmenFarbe,
                                style = Stroke(width = 3f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                            for ((i, p) in ps.withIndex()) {
                                val mitte = bildPs[i]
                                val gewaehlt = p.id == viewModel.auswahlGegenstandPunkt
                                val r = (if (gewaehlt) 13f else 10f) * dichte
                                if (p.rund) {
                                    drawCircle(hintergrund, radius = r, center = mitte)
                                    drawCircle(rahmenFarbe, radius = r, center = mitte, style = Stroke(width = 2.5f * dichte))
                                    if (gewaehlt) drawCircle(rahmenFarbe, radius = r * 0.55f, center = mitte)
                                } else {
                                    val ecke = Offset(mitte.x - r, mitte.y - r)
                                    drawRect(hintergrund, topLeft = ecke, size = Size(2f * r, 2f * r))
                                    drawRect(rahmenFarbe, topLeft = ecke, size = Size(2f * r, 2f * r), style = Stroke(width = 2.5f * dichte))
                                    if (gewaehlt) {
                                        drawRect(
                                            rahmenFarbe,
                                            topLeft = Offset(mitte.x - r * 0.55f, mitte.y - r * 0.55f),
                                            size = Size(1.1f * r, 1.1f * r),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val w2 = ausgewaehlt.breite * faktor / 2f
                        val h2 = ausgewaehlt.hoehe * faktor / 2f
                        val ecken = listOf(
                            gBild(ausgewaehlt, -w2, -h2),
                            gBild(ausgewaehlt, w2, -h2),
                            gBild(ausgewaehlt, w2, h2),
                            gBild(ausgewaehlt, -w2, h2),
                        )
                        for (i in 0 until 4) {
                            drawLine(
                                rahmenFarbe,
                                ecken[i],
                                ecken[(i + 1) % 4],
                                strokeWidth = 3f * dichte,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    for (gr in gGriffe(ausgewaehlt)) {
                        val stiel = gr.stiel
                        if (stiel != null) drawLine(rahmenFarbe, stiel, gr.pos, strokeWidth = 2f * dichte)
                        if (gr.drehen) {
                            drawCircle(hintergrund, radius = 11f * dichte, center = gr.pos)
                            drawCircle(rahmenFarbe, radius = 11f * dichte, center = gr.pos, style = Stroke(width = 2.5f * dichte))
                            drawCircle(rahmenFarbe, radius = 4f * dichte, center = gr.pos)
                        } else {
                            val r = (if (gr.sx != 0 && gr.sy != 0) 8f else 6f) * dichte
                            val ecke = Offset(gr.pos.x - r, gr.pos.y - r)
                            drawRect(hintergrund, topLeft = ecke, size = Size(2f * r, 2f * r))
                            drawRect(rahmenFarbe, topLeft = ecke, size = Size(2f * r, 2f * r), style = Stroke(width = 2.5f * dichte))
                        }
                    }
                }

                // Angefangene Fläche: offene Linie durch die Punkte, erst ein Tipp auf den ersten Punkt schließt sie.
                if (zeichnung != null) {
                    val bild = zeichnung.map { zuBildschirm(it) }
                    val mitVorschau = if (vorschau != null) bild + zuBildschirm(vorschau.punkt) else bild
                    if (mitVorschau.size >= 2) {
                        val pfad = kurvenPfad(mitVorschau, List(mitVorschau.size) { viewModel.zeichnungRund }, geschlossen = false)
                        drawPath(
                            pfad,
                            rahmenFarbe,
                            style = Stroke(width = 3f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                    // Hilfslinien der Ausrichtungshilfe
                    if (vorschau != null) {
                        val gestrichelt = PathEffect.dashPathEffect(floatArrayOf(12f * dichte, 8f * dichte))
                        for ((von, bis) in vorschau.hilfslinien) {
                            drawLine(
                                rahmenFarbe.copy(alpha = 0.8f),
                                zuBildschirm(von),
                                zuBildschirm(bis),
                                strokeWidth = 2f * dichte,
                                pathEffect = gestrichelt,
                            )
                        }
                        drawCircle(rahmenFarbe, radius = 6f * dichte, center = zuBildschirm(vorschau.punkt))
                        drawCircle(hintergrund, radius = 3f * dichte, center = zuBildschirm(vorschau.punkt))
                    }
                    bild.forEachIndexed { i, p ->
                        drawCircle(rahmenFarbe, radius = (if (i == 0) 9f else 6f) * dichte, center = p)
                    }
                    // Ring um den ersten Punkt: ab drei Punkten kann man hier tippen, um zu schließen.
                    if (bild.size >= 3) {
                        drawCircle(
                            rahmenFarbe,
                            radius = (if (nahErstemPunkt) SCHLIESS_RADIUS_DP else 16f) * dichte,
                            center = bild[0],
                            style = Stroke(width = 2f * dichte),
                        )
                    }
                }

                // Vorschau einer aufgezogenen Form
                val start = formStart
                val ende = zeigerPos
                if (formAuswahl != null && start != null && ende != null) {
                    val vorschauPunkte = formPunkte(formAuswahl, start, fangFremd(zuSkizze(ende)), 0.001f)
                    if (vorschauPunkte.size >= 3) {
                        val pfad = kurvenPfad(
                            vorschauPunkte.map { zuBildschirm(it.lage) },
                            vorschauPunkte.map { it.rund },
                            geschlossen = true,
                        )
                        drawPath(pfad, rahmenFarbe.copy(alpha = 0.25f))
                        drawPath(
                            pfad,
                            rahmenFarbe,
                            style = Stroke(width = 3f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }

                // Einrastmarken an fremden Ecken und Kanten
                for (m in fangMarken) {
                    val c = zuBildschirm(m)
                    drawCircle(FANG_FARBE, radius = 9f * dichte, center = c, style = Stroke(width = 3f * dichte))
                    drawCircle(FANG_FARBE, radius = 3f * dichte, center = c)
                }
              }
              zeichneSzene()

                // Lupe: vergrößerter Ausschnitt über dem Finger, solange gezogen oder gezeichnet wird
                val lf = lupeFokus
                val fingerLupe = zeigerPos
                if (lf != null && fingerLupe != null) {
                    val r = LUPE_RADIUS_DP * dichte
                    val rand = 6f * dichte
                    val cx = fingerLupe.x.coerceIn(r + rand, size.width - r - rand)
                    var cy = fingerLupe.y - r - 84f * dichte
                    if (cy < r + 36f * dichte) cy = (fingerLupe.y + r + 84f * dichte).coerceAtMost(size.height - r - rand)
                    val mitteLupe = Offset(cx, cy)
                    drawCircle(Color(0x55000000), radius = r + 3f * dichte, center = mitteLupe + Offset(0f, 2f * dichte))
                    clipPath(Path().apply { addOval(Rect(center = mitteLupe, radius = r)) }) {
                        drawRect(hintergrund, topLeft = Offset(cx - r, cy - r), size = Size(2f * r, 2f * r))
                        withTransform({
                            translate(cx - lf.x, cy - lf.y)
                            scale(LUPE_FAKTOR, LUPE_FAKTOR, pivot = lf)
                        }) { zeichneSzene() }
                    }
                    drawCircle(rahmenFarbe, radius = r, center = mitteLupe, style = Stroke(width = 3f * dichte))
                    val arm = 8f * dichte
                    drawLine(Color(0xCC000000), Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.5f * dichte)
                    drawLine(Color(0xCC000000), Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.5f * dichte)
                }

                // Maß oder Winkel neben dem Finger, solange Größe oder Drehung geändert wird
                val massGz = gZiehen
                if (liveText != null && massGz != null) {
                    massPinsel.textSize = 15f * dichte
                    val textBreite = massPinsel.measureText(liveText)
                    val boxB = textBreite + 20f * dichte
                    val boxH = 28f * dichte
                    val roh = zuBildschirm(massGz.aktuell) + Offset(0f, -52f * dichte)
                    val mx = roh.x.coerceIn(boxB / 2f + 4f * dichte, size.width - boxB / 2f - 4f * dichte)
                    val my = roh.y.coerceAtLeast(boxH / 2f + 30f * dichte)
                    drawRoundRect(
                        Color(0xE6222222),
                        topLeft = Offset(mx - boxB / 2f, my - boxH / 2f),
                        size = Size(boxB, boxH),
                        cornerRadius = CornerRadius(8f * dichte),
                    )
                    drawIntoCanvas { it.nativeCanvas.drawText(liveText, mx, my + 5f * dichte, massPinsel) }
                }

                if (bearbeiten) {
                    zeichneLineale(
                        zoom = zoom,
                        dichte = dichte,
                        massstab = garten.massstab,
                        verschiebung = Offset(verschiebungX, verschiebungY),
                        hintergrund = linealHintergrund,
                        linie = linealLinie,
                        textFarbe = linealText,
                        pinsel = linealPinsel,
                    )
                }
            }

            val info = flaechen.firstOrNull { it.id == infoFlaecheId }
            if (!bearbeiten && info != null) {
                val art = Oberflaeche.vonSchluessel(info.oberflaeche)
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = info.name ?: stringResource(R.string.flaeche_ohne_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Farbfeld(art, info.farbe)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(art.nameRes), style = MaterialTheme.typography.bodyMedium)
                        }
                        val ihre = punkteJeFlaeche[info.id].orEmpty()
                        if (ihre.size >= 3) {
                            val masse = flaechenMasse(
                                kurvenPolylinie(ihre.map { Offset(it.x, it.y) }, ihre.map { it.rund }, true),
                                garten.massstab,
                            )
                            Text(
                                text = stringResource(
                                    R.string.info_masse,
                                    zahlText(masse.breite),
                                    zahlText(masse.hoehe),
                                    zahlText(masse.flaeche),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            val infoG = gegenstaende.firstOrNull { it.id == infoGegenstandId }
            if (!bearbeiten && infoG != null) {
                val gart = GegenstandsArt.vonSchluessel(infoG.art)
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = infoG.name ?: stringResource(gart.nameRes),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (infoG.name != null) {
                            Text(stringResource(gart.nameRes), style = MaterialTheme.typography.bodyMedium)
                        }
                        val infoPunkte = gpJeGegenstand[infoG.id].orEmpty()
                        if (istGebaeude(infoG) && infoPunkte.size >= 3) {
                            val masse = flaechenMasse(
                                kurvenPolylinie(infoPunkte.map { Offset(it.x, it.y) }, infoPunkte.map { it.rund }, true),
                                garten.massstab,
                            )
                            Text(
                                text = stringResource(
                                    R.string.info_masse,
                                    zahlText(masse.breite),
                                    zahlText(masse.hoehe),
                                    zahlText(masse.flaeche),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.masse_live,
                                    zahlText(infoG.breite / garten.massstab),
                                    zahlText(infoG.hoehe / garten.massstab),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            val gMenuePos = gegenstandMenue
            if (gMenuePos != null && gewaehlterGegenstand != null) {
                Box(modifier = Modifier.offset { IntOffset(gMenuePos.x.toInt(), gMenuePos.y.toInt()) }) {
                    DropdownMenu(expanded = true, onDismissRequest = { gegenstandMenue = null }) {
                        MenuePunkt(R.string.menue_name_aendern) {
                            gegenstandMenue = null
                            gNamenDialog = true
                        }
                        MenuePunkt(R.string.menue_farbe) {
                            gegenstandMenue = null
                            gFarbeDialog = true
                        }
                        MenuePunkt(R.string.menue_duplizieren) {
                            gegenstandMenue = null
                            viewModel.dupliziereGegenstand(gartenId, gewaehlterGegenstand.id, garten.massstab)
                        }
                        MenuePunkt(R.string.menue_drehen90) {
                            gegenstandMenue = null
                            viewModel.setzeGegenstandLage(
                                gartenId,
                                gewaehlterGegenstand.copy(drehung = (gewaehlterGegenstand.drehung + 90f) % 360f),
                            )
                        }
                        MenuePunkt(R.string.menue_nach_vorn) {
                            gegenstandMenue = null
                            viewModel.bewegeGegenstandInNachbarebene(gartenId, gewaehlterGegenstand.id, 1)
                        }
                        MenuePunkt(R.string.menue_nach_hinten) {
                            gegenstandMenue = null
                            viewModel.bewegeGegenstandInNachbarebene(gartenId, gewaehlterGegenstand.id, -1)
                        }
                        MenuePunkt(R.string.loeschen) {
                            gegenstandMenue = null
                            gLoeschenDialog = true
                        }
                    }
                }
            }

            val menuePos = flaechenMenue
            if (menuePos != null && gewaehlteFlaeche != null) {
                Box(modifier = Modifier.offset { IntOffset(menuePos.x.toInt(), menuePos.y.toInt()) }) {
                    DropdownMenu(expanded = true, onDismissRequest = { flaechenMenue = null }) {
                        MenuePunkt(R.string.menue_name_aendern) {
                            flaechenMenue = null
                            namenDialog = true
                        }
                        MenuePunkt(R.string.menue_oberflaeche) {
                            flaechenMenue = null
                            oberflaecheDialog = true
                        }
                        MenuePunkt(R.string.menue_farbe) {
                            flaechenMenue = null
                            flaechenFarbeDialog = true
                        }
                        MenuePunkt(R.string.menue_nach_vorn) {
                            flaechenMenue = null
                            viewModel.bewegeFlaecheInNachbarebene(gartenId, gewaehlteFlaeche.id, 1)
                        }
                        MenuePunkt(R.string.menue_nach_hinten) {
                            flaechenMenue = null
                            viewModel.bewegeFlaecheInNachbarebene(gartenId, gewaehlteFlaeche.id, -1)
                        }
                        MenuePunkt(R.string.alle_rund) {
                            flaechenMenue = null
                            viewModel.setzeAlleRund(gartenId, gewaehlteFlaeche.id, true)
                        }
                        MenuePunkt(R.string.alle_eckig) {
                            flaechenMenue = null
                            viewModel.setzeAlleRund(gartenId, gewaehlteFlaeche.id, false)
                        }
                        MenuePunkt(R.string.loeschen) {
                            flaechenMenue = null
                            loeschenDialog = true
                        }
                    }
                }
            }

            if (bearbeiten && !zeichnet && ebenenOffen) {
                // Unsichtbare Schicht: ein Tipp neben die Liste schließt sie wieder.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { ebenenOffen = false } },
                )
            }

            if (bearbeiten && !zeichnet) {
                EbenenBereich(
                    ebenen = ebenen,
                    flaechen = flaechen,
                    gegenstaende = gegenstaende,
                    aktiveId = aktiveEbeneId,
                    gewaehlteFlaecheId = viewModel.auswahlFlaeche,
                    gewaehlterGegenstandId = viewModel.auswahlGegenstand,
                    offen = ebenenOffen,
                    onSchalteOffen = { ebenenOffen = !ebenenOffen },
                    onWaehleEbene = { viewModel.waehleEbene(it) },
                    onWaehleFlaeche = { viewModel.waehleFlaeche(it) },
                    onWaehleGegenstand = { viewModel.waehleGegenstand(it) },
                    verdecktZeigen = viewModel.verdecktZeigen,
                    onVerdecktSchalten = viewModel::schalteVerdecktZeigen,
                    onEbenePlatz = { id, platz -> viewModel.setzeEbenePlatz(gartenId, id, platz) },
                    onOrdneElemente = { id, ordnung -> viewModel.ordneElemente(gartenId, id, ordnung) },
                    onElementInEbene = { key, ziel, ordnung -> viewModel.verschiebeElementInEbene(gartenId, key, ziel, ordnung) },
                    onNeueEbene = { viewModel.legeEbeneAn(gartenId) },
                    onUmbenennen = { ebeneUmbenennenId = it },
                    onNachVorn = { viewModel.bewegeEbene(gartenId, it, 1) },
                    onNachHinten = { viewModel.bewegeEbene(gartenId, it, -1) },
                    onLoeschen = { id ->
                        // Eine leere Ebene wird gleich gelöscht, sonst wird gefragt, was mit dem Inhalt geschieht.
                        if (flaechen.none { it.ebeneId == id } && gegenstaende.none { it.ebeneId == id }) {
                            viewModel.loescheEbene(gartenId, id, true)
                        } else {
                            ebeneLoeschenId = id
                        }
                    },
                    onSicht = { id, aktuell -> viewModel.schalteEbeneSicht(gartenId, id, aktuell) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = 32.dp, bottom = 84.dp, end = 8.dp),
                )
            }

            FilledTonalButton(
                onClick = {
                    zoom = 1f
                    verschiebungX = 0f
                    verschiebungY = 0f
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Text(stringResource(R.string.ansicht_zuruecksetzen))
            }
        }
    }

    val ebeneUmbenennen = ebenen.firstOrNull { it.id == ebeneUmbenennenId }
    if (ebeneUmbenennen != null) {
        NameDialog(
            titel = stringResource(R.string.ebene_name_titel),
            startwert = ebeneUmbenennen.name.orEmpty(),
            bestaetigenText = stringResource(R.string.speichern),
            onBestaetigt = { name ->
                ebeneUmbenennenId = null
                viewModel.benenneEbeneUm(gartenId, ebeneUmbenennen.id, name)
            },
            onAbbruch = { ebeneUmbenennenId = null },
        )
    }
    val ebeneLoeschen = ebenen.firstOrNull { it.id == ebeneLoeschenId }
    if (ebeneLoeschen != null) {
        val anzahlDrin = flaechen.count { it.ebeneId == ebeneLoeschen.id } +
            gegenstaende.count { it.ebeneId == ebeneLoeschen.id }
        AlertDialog(
            onDismissRequest = { ebeneLoeschenId = null },
            title = { Text(stringResource(R.string.ebene_loeschen_titel)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.ebene_loeschen_text, anzahlDrin))
                    Button(
                        onClick = {
                            ebeneLoeschenId = null
                            viewModel.loescheEbene(gartenId, ebeneLoeschen.id, true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.ebene_inhalt_verschieben)) }
                    OutlinedButton(
                        onClick = {
                            ebeneLoeschenId = null
                            viewModel.loescheEbene(gartenId, ebeneLoeschen.id, false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.ebene_inhalt_loeschen)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { ebeneLoeschenId = null }) { Text(stringResource(R.string.abbrechen)) }
            },
        )
    }
    val ortFuerKatalog = platzOrt
    if (ortFuerKatalog != null && viewModel.platzieren) {
        KatalogDialog(
            onWahl = { art ->
                platzOrt = null
                viewModel.legeGegenstandAn(
                    gartenId,
                    Gegenstand(
                        gartenId = gartenId,
                        ebeneId = 0,
                        art = art.schluessel,
                        mitteX = ortFuerKatalog.x,
                        mitteY = ortFuerKatalog.y,
                        breite = art.breiteM * garten.massstab,
                        hoehe = art.hoeheM * garten.massstab,
                    ),
                    if (art == GegenstandsArt.GEBAEUDE_FREI) {
                        // Zu Beginn ein Rechteck aus vier Ecken, relativ zur Mitte
                        val hb = art.breiteM * garten.massstab / 2f
                        val hh = art.hoeheM * garten.massstab / 2f
                        listOf(-hb to -hh, hb to -hh, hb to hh, -hb to hh).mapIndexed { nr, (px, py) ->
                            Gegenstandspunkt(gegenstandId = 0, nr = nr, x = px, y = py, rund = false)
                        }
                    } else {
                        emptyList()
                    },
                )
            },
            onAbbrechen = { platzOrt = null },
        )
    }
    if (flaechenFarbeDialog && gewaehlteFlaeche != null) {
        FarbDialog(
            aktuell = gewaehlteFlaeche.farbe,
            onWahl = { farbe ->
                flaechenFarbeDialog = false
                viewModel.setzeFlaechenFarbe(gartenId, gewaehlteFlaeche.id, farbe)
            },
            onAbbrechen = { flaechenFarbeDialog = false },
        )
    }
    if (gFarbeDialog && gewaehlterGegenstand != null) {
        FarbDialog(
            aktuell = gewaehlterGegenstand.farbe,
            onWahl = { farbe ->
                gFarbeDialog = false
                viewModel.setzeGegenstandFarbe(gartenId, gewaehlterGegenstand.id, farbe)
            },
            onAbbrechen = { gFarbeDialog = false },
        )
    }
    if (gNamenDialog && gewaehlterGegenstand != null) {
        NameDialog(
            titel = stringResource(R.string.gegenstand_name_titel),
            startwert = gewaehlterGegenstand.name.orEmpty(),
            bestaetigenText = stringResource(R.string.speichern),
            onBestaetigt = { name ->
                gNamenDialog = false
                viewModel.benenneGegenstandUm(gartenId, gewaehlterGegenstand.id, name)
            },
            onAbbruch = { gNamenDialog = false },
        )
    }
    if (gLoeschenDialog && gewaehlterGegenstand != null) {
        BestaetigenDialog(
            titel = stringResource(R.string.gegenstand_loeschen_titel),
            text = stringResource(R.string.gegenstand_loeschen_text),
            bestaetigenText = stringResource(R.string.loeschen),
            onBestaetigt = {
                gLoeschenDialog = false
                viewModel.loescheGegenstand(gartenId, gewaehlterGegenstand.id)
            },
            onAbbruch = { gLoeschenDialog = false },
        )
    }
    if (formenDialog) {
        FormenDialog(
            onWahl = { form ->
                formenDialog = false
                viewModel.waehleForm(form)
            },
            onAbbrechen = { formenDialog = false },
        )
    }
    if (hilfeDialog) {
        HilfeDialog(
            bearbeiten = bearbeiten,
            zeichnet = zeichnung != null,
            formModus = formAuswahl != null,
            flaecheGewaehlt = gewaehlteFlaeche != null,
            gegenstandGewaehlt = gewaehlterGegenstand != null,
            gebaeudeGewaehlt = gewaehlterGegenstand?.let { istGebaeude(it) } == true,
            platziert = viewModel.platzieren,
            onSchliessen = { hilfeDialog = false },
        )
    }
    // Gleich nach dem Anlegen einer Fläche: Oberfläche wählen (Schließen ohne Wahl behält Gras).
    val neueFlaecheId = viewModel.neueFlaecheId
    if (neueFlaecheId != null) {
        OberflaechenDialog(
            aktuell = null,
            onWahl = { art -> viewModel.waehleOberflaecheFuerNeue(gartenId, neueFlaecheId, art.schluessel) },
            onAbbrechen = { viewModel.beendeOberflaechenWahl() },
        )
    }
    if (oberflaecheDialog && gewaehlteFlaeche != null) {
        OberflaechenDialog(
            aktuell = Oberflaeche.vonSchluessel(gewaehlteFlaeche.oberflaeche),
            onWahl = { art ->
                oberflaecheDialog = false
                viewModel.setzeOberflaeche(gartenId, gewaehlteFlaeche.id, art.schluessel)
            },
            onAbbrechen = { oberflaecheDialog = false },
        )
    }
    if (namenDialog && gewaehlteFlaeche != null) {
        NameDialog(
            titel = stringResource(R.string.flaeche_name_titel),
            startwert = gewaehlteFlaeche.name.orEmpty(),
            bestaetigenText = stringResource(R.string.speichern),
            onBestaetigt = { name ->
                namenDialog = false
                viewModel.benenneFlaecheUm(gartenId, gewaehlteFlaeche.id, name)
            },
            onAbbruch = { namenDialog = false },
        )
    }
    if (loeschenDialog && gewaehlteFlaeche != null) {
        BestaetigenDialog(
            titel = stringResource(R.string.flaeche_loeschen_titel),
            text = stringResource(R.string.flaeche_loeschen_text),
            bestaetigenText = stringResource(R.string.loeschen),
            onBestaetigt = {
                loeschenDialog = false
                viewModel.loescheFlaeche(gartenId, gewaehlteFlaeche.id)
            },
            onAbbruch = { loeschenDialog = false },
        )
    }
}

private val KNOPF_ABSTAND = PaddingValues(horizontal = 4.dp, vertical = 8.dp)

@Composable
private fun KnopfVoll(textId: Int, onClick: () -> Unit, modifier: Modifier) {
    Button(onClick = onClick, modifier = modifier, contentPadding = KNOPF_ABSTAND) {
        Text(stringResource(textId), maxLines = 1, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun KnopfUmriss(textId: Int, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, contentPadding = KNOPF_ABSTAND) {
        Text(stringResource(textId), maxLines = 1, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Werkzeugleiste(
    zeichnung: List<Offset>?,
    zeichnungRund: Boolean,
    formAuswahl: Form?,
    einrasten: Boolean,
    kannRueckgaengig: Boolean,
    gewaehlteFlaeche: Flaeche?,
    gegenstandGewaehlt: Boolean,
    gebaeudePunkt: Gegenstandspunkt?,
    gebaeudePunktLoeschenMoeglich: Boolean,
    onGebaeudePunktLoeschen: () -> Unit,
    onGebaeudePunktRund: () -> Unit,
    platzieren: Boolean,
    gewaehlterPunkt: Punkt?,
    punktLoeschenMoeglich: Boolean,
    onFlaecheZeichnen: () -> Unit,
    onFormenWaehlen: () -> Unit,
    onGegenstand: () -> Unit,
    onPlatzierenAbbrechen: () -> Unit,
    onRueckgaengig: () -> Unit,
    onAbbrechen: () -> Unit,
    onSchalteRund: () -> Unit,
    onSchalteEinrasten: () -> Unit,
    onAbwaehlen: () -> Unit,
    onPunktLoeschen: () -> Unit,
    onPunktRundSchalten: () -> Unit,
) {
    val einrastenText = if (einrasten) R.string.einrasten_an else R.string.einrasten_aus
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (zeichnung != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnopfUmriss(R.string.abbrechen, onAbbrechen, Modifier.weight(1f))
                    KnopfUmriss(R.string.rueckgaengig, onRueckgaengig, Modifier.weight(1f), enabled = zeichnung.isNotEmpty())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnopfUmriss(
                        if (zeichnungRund) R.string.alle_eckig else R.string.alle_rund,
                        onSchalteRund,
                        Modifier.weight(1f),
                    )
                    KnopfUmriss(einrastenText, onSchalteEinrasten, Modifier.weight(1f))
                }
            } else if (formAuswahl != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnopfUmriss(R.string.abbrechen, onAbbrechen, Modifier.weight(1f))
                    KnopfUmriss(R.string.andere_form, onFormenWaehlen, Modifier.weight(1f))
                }
            } else if (platzieren) {
                Text(stringResource(R.string.platzieren_hinweis), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnopfUmriss(R.string.abbrechen, onPlatzierenAbbrechen, Modifier.weight(1f))
                }
            } else if (gegenstandGewaehlt) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (gebaeudePunkt != null) {
                        KnopfUmriss(R.string.punkt_loeschen, onGebaeudePunktLoeschen, Modifier.weight(1f), enabled = gebaeudePunktLoeschenMoeglich)
                        KnopfUmriss(
                            if (gebaeudePunkt.rund) R.string.punkt_zu_ecke else R.string.punkt_zu_rund,
                            onGebaeudePunktRund,
                            Modifier.weight(1f),
                        )
                    } else {
                        KnopfUmriss(R.string.abwaehlen, onAbwaehlen, Modifier.weight(1f))
                        KnopfUmriss(einrastenText, onSchalteEinrasten, Modifier.weight(1f))
                    }
                    KnopfUmriss(R.string.rueckgaengig, onRueckgaengig, Modifier.weight(1f), enabled = kannRueckgaengig)
                }
            } else if (gewaehlteFlaeche != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (gewaehlterPunkt != null) {
                        KnopfUmriss(R.string.punkt_loeschen, onPunktLoeschen, Modifier.weight(1f), enabled = punktLoeschenMoeglich)
                        KnopfUmriss(
                            if (gewaehlterPunkt.rund) R.string.punkt_zu_ecke else R.string.punkt_zu_rund,
                            onPunktRundSchalten,
                            Modifier.weight(1f),
                        )
                    } else {
                        KnopfUmriss(R.string.abwaehlen, onAbwaehlen, Modifier.weight(1f))
                        KnopfUmriss(einrastenText, onSchalteEinrasten, Modifier.weight(1f))
                    }
                    KnopfUmriss(R.string.rueckgaengig, onRueckgaengig, Modifier.weight(1f), enabled = kannRueckgaengig)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KnopfVoll(R.string.zeichnen, onFlaecheZeichnen, Modifier.weight(1f))
                    KnopfVoll(R.string.form_waehlen, onFormenWaehlen, Modifier.weight(1f))
                    KnopfVoll(R.string.gegenstand_knopf, onGegenstand, Modifier.weight(1.25f))
                    KnopfUmriss(R.string.rueckgaengig, onRueckgaengig, Modifier.weight(1.25f), enabled = kannRueckgaengig)
                }
            }
        }
    }
}

@Composable
private fun MenuePunkt(textId: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(textId)) }, onClick = onClick)
}

/** Katalog der Gegenstände: Raster mit kleinen Symbolen, nach Gruppen geordnet. */
@Composable
private fun KatalogDialog(onWahl: (GegenstandsArt) -> Unit, onAbbrechen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        title = { Text(stringResource(R.string.katalog_titel)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (gruppe in GegenstandsGruppe.values()) {
                    Text(
                        text = stringResource(gruppe.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    val arten = GegenstandsArt.values().filter { it.gruppe == gruppe }
                    for (zeile in arten.chunked(3)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (art in zeile) KatalogFeld(art, onWahl, Modifier.weight(1f))
                            repeat(3 - zeile.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onAbbrechen) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

@Composable
private fun KatalogFeld(art: GegenstandsArt, onWahl: (GegenstandsArt) -> Unit, modifier: Modifier) {
    val dichte = LocalDensity.current.density
    Column(
        modifier = modifier.clickable { onWahl(art) }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val skala = size.minDimension / maxOf(art.breiteM, art.hoeheM)
            val w = art.breiteM * skala
            val h = art.hoeheM * skala
            // Sehr schmale Gegenstände (Zaun, ...) werden in der Vorschau etwas dicker gezeichnet.
            val hVorschau = maxOf(h, 10f * dichte)
            translate((size.width - w) / 2f, (size.height - hVorschau) / 2f) {
                zeichneGegenstand(art, w, hVorschau, art.farbe, dichte, skala)
            }
        }
        Text(
            text = stringResource(art.nameRes),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun FormenDialog(onWahl: (Form) -> Unit, onAbbrechen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text(stringResource(R.string.form_dialog_titel)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (form in Form.values()) {
                    TextButton(onClick = { onWahl(form) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(form.nameRes))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onAbbrechen) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

/** Hilfetext, der zur aktuellen Lage passt (Ansicht, Zeichnen, Form, Auswahl). */
@Composable
private fun HilfeDialog(
    bearbeiten: Boolean,
    zeichnet: Boolean,
    formModus: Boolean,
    flaecheGewaehlt: Boolean,
    gegenstandGewaehlt: Boolean,
    gebaeudeGewaehlt: Boolean,
    platziert: Boolean,
    onSchliessen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text(stringResource(R.string.hilfe_titel)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!bearbeiten) {
                    Text(stringResource(R.string.hilfe_ansicht))
                } else {
                    Text(stringResource(R.string.hilfe_bearbeiten))
                    if (zeichnet) Text(stringResource(R.string.hilfe_zeichnen))
                    if (formModus) Text(stringResource(R.string.hilfe_form))
                    if (flaecheGewaehlt) Text(stringResource(R.string.hilfe_auswahl))
                    if (platziert) Text(stringResource(R.string.hilfe_platzieren))
                    if (gegenstandGewaehlt) Text(stringResource(R.string.hilfe_gegenstand))
                    if (gegenstandGewaehlt) Text(stringResource(R.string.hilfe_schmal))
                    if (gebaeudeGewaehlt) Text(stringResource(R.string.hilfe_gebaeude))
                    if (!zeichnet && !formModus && !flaecheGewaehlt && !platziert && !gegenstandGewaehlt) {
                        Text(stringResource(R.string.hilfe_werkzeuge))
                    }
                    if (!zeichnet && !formModus) Text(stringResource(R.string.hilfe_ebenen))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchliessen) { Text(stringResource(R.string.ok)) }
        },
    )
}

/**
 * Eigene Gestenerkennung.
 *  - Zwei Finger zoomen und verschieben immer.
 *  - [einFingerVerschiebt]: ein Finger verschiebt die Ansicht (nur in der Ansicht).
 *  - [einFingerAktion]: ein Finger zeichnet, wählt aus oder zieht (Bearbeitungsmodus). Solange
 *    er liegt, meldet [beiZeiger] seine Position; beim Loslassen kommt [beiLoslassen]. Kommt ein
 *    zweiter Finger dazu, wird abgebrochen ([beiZeiger] mit null, kein [beiLoslassen]).
 *    Am Ende jeder Geste kommt noch einmal [beiZeiger] mit null.
 *  - In der Ansicht ([einFingerAktion] aus) meldet ein Tipp (ein Finger, kaum bewegt) [beiTipp].
 *  - Bleibt der Finger im Bearbeitungsmodus lange ruhig liegen, kommt [beiLangemDruck]. Danach
 *    ist die Geste beendet (kein Ziehen, kein Loslassen-Ereignis mehr).
 */
private suspend fun PointerInputScope.gartenGesten(
    einFingerVerschiebt: Boolean,
    einFingerAktion: Boolean,
    beiZeiger: (Offset?) -> Unit,
    beiLoslassen: (Offset) -> Unit,
    beiLangemDruck: (Offset) -> Unit,
    beiTipp: (Offset) -> Unit,
    beiTransform: (schwerpunkt: Offset, verschieben: Offset, zoomFaktor: Float) -> Unit,
) {
    awaitEachGesture {
        val runter = awaitFirstDown(requireUnconsumed = false)
        val startZeit = System.currentTimeMillis()
        val langDauer = viewConfiguration.longPressTimeoutMillis
        val schwelle = viewConfiguration.touchSlop
        var maxFinger = 1
        var letztePosition = runter.position
        var zuWeitBewegt = false
        var langerDruck = false
        if (einFingerAktion) beiZeiger(runter.position)
        while (true) {
            val warteAufLang = einFingerAktion && !langerDruck && !zuWeitBewegt && maxFinger == 1
            val ereignis: PointerEvent? = if (warteAufLang) {
                val rest = maxOf(1L, langDauer - (System.currentTimeMillis() - startZeit))
                withTimeoutOrNull(rest) { awaitPointerEvent() }
            } else {
                awaitPointerEvent()
            }
            if (ereignis == null) {
                // Der Finger liegt lange ruhig: langer Druck
                langerDruck = true
                beiZeiger(null)
                beiLangemDruck(letztePosition)
                continue
            }
            val fingerAnzahl = ereignis.changes.count { it.pressed }
            if (fingerAnzahl > maxFinger) maxFinger = fingerAnzahl
            if (fingerAnzahl >= 2) {
                if (einFingerAktion && !langerDruck) beiZeiger(null)
                beiTransform(
                    ereignis.calculateCentroid(useCurrent = false),
                    ereignis.calculatePan(),
                    ereignis.calculateZoom(),
                )
                ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
            } else if (fingerAnzahl == 1) {
                val finger = ereignis.changes.first { it.pressed }
                letztePosition = finger.position
                if ((finger.position - runter.position).getDistance() > schwelle) zuWeitBewegt = true
                if (einFingerAktion) {
                    if (!langerDruck && maxFinger == 1) beiZeiger(finger.position)
                    ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                } else if (einFingerVerschiebt && zuWeitBewegt) {
                    beiTransform(finger.position, ereignis.calculatePan(), 1f)
                    ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
            if (ereignis.changes.none { it.pressed }) break
        }
        if (einFingerAktion) {
            if (maxFinger == 1 && !langerDruck) beiLoslassen(letztePosition)
            beiZeiger(null)
        } else if (maxFinger == 1 && !zuWeitBewegt) {
            beiTipp(letztePosition)
        }
    }
}

/** Schritte für Gitter und Lineale in Metern (1, 2, 5 je Zehnerpotenz). */
private val NETZ_METER = floatArrayOf(0.1f, 0.2f, 0.5f, 1f, 2f, 5f, 10f, 20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f)

/** Kleinster Schritt, der auf dem Bildschirm mindestens [mindestPx] breit ist. */
private fun netzIndex(pixelProMeter: Float, mindestPx: Float): Int {
    for (i in NETZ_METER.indices) if (NETZ_METER[i] * pixelProMeter >= mindestPx) return i
    return NETZ_METER.lastIndex
}

/** In wie viele kleine Teile ein Schritt geteilt wird (bei 2er-Schritten vier, sonst fünf). */
private fun netzTeilung(index: Int): Int = if (index % 3 == 1) 4 else 5

private fun zahlText(v: Float): String =
    if (v >= 100f) String.format(Locale.getDefault(), "%.0f", v) else String.format(Locale.getDefault(), "%.1f", v)

private fun meterText(v: Float): String =
    if (abs(v - v.roundToInt()) < 0.001f) v.roundToInt().toString() else String.format(Locale.getDefault(), "%.1f", v)

/** Breite, Höhe und ungefähre Fläche einer Fläche in Metern (Näherung über den Umriss). */
private data class FlaechenMasse(val breite: Float, val hoehe: Float, val flaeche: Float)

private fun flaechenMasse(umriss: List<Offset>, einheitenProMeter: Float): FlaechenMasse {
    if (umriss.size < 3) return FlaechenMasse(0f, 0f, 0f)
    var minX = umriss[0].x
    var maxX = minX
    var minY = umriss[0].y
    var maxY = minY
    var summe = 0f
    for (i in umriss.indices) {
        val a = umriss[i]
        val b = umriss[(i + 1) % umriss.size]
        if (a.x < minX) minX = a.x
        if (a.x > maxX) maxX = a.x
        if (a.y < minY) minY = a.y
        if (a.y > maxY) maxY = a.y
        summe += a.x * b.y - b.x * a.y
    }
    val m = einheitenProMeter
    return FlaechenMasse((maxX - minX) / m, (maxY - minY) / m, abs(summe) / 2f / (m * m))
}

/**
 * Zeichnet ein Hilfsgitter in Metern und den Nullpunkt als Kreuz. Die großen Linien liegen
 * auf runden Metern (1, 2, 5, 10, ... je nach Zoom), dazwischen liegen feine Linien.
 */
private fun DrawScope.zeichneGitter(
    zoom: Float,
    dichte: Float,
    massstab: Float,
    verschiebung: Offset,
    gitterFarbe: Color,
    achsenFarbe: Color,
) {
    val pixelProMeter = zoom * dichte * massstab
    val index = netzIndex(pixelProMeter, 56f * dichte)
    val teilung = netzTeilung(index)
    val gross = NETZ_METER[index] * pixelProMeter
    val klein = gross / teilung

    if (klein >= 10f * dichte) {
        val fein = gitterFarbe.copy(alpha = 0.4f)
        val ersteX = floor(-verschiebung.x / klein).toInt()
        val letzteX = floor((size.width - verschiebung.x) / klein).toInt() + 1
        for (i in ersteX..letzteX) {
            if (i.mod(teilung) == 0) continue
            val x = verschiebung.x + i * klein
            drawLine(fein, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        val ersteY = floor(-verschiebung.y / klein).toInt()
        val letzteY = floor((size.height - verschiebung.y) / klein).toInt() + 1
        for (i in ersteY..letzteY) {
            if (i.mod(teilung) == 0) continue
            val y = verschiebung.y + i * klein
            drawLine(fein, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
    }

    val ersteX = floor(-verschiebung.x / gross).toInt()
    val letzteX = floor((size.width - verschiebung.x) / gross).toInt() + 1
    for (i in ersteX..letzteX) {
        val x = verschiebung.x + i * gross
        drawLine(gitterFarbe, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    }
    val ersteY = floor(-verschiebung.y / gross).toInt()
    val letzteY = floor((size.height - verschiebung.y) / gross).toInt() + 1
    for (i in ersteY..letzteY) {
        val y = verschiebung.y + i * gross
        drawLine(gitterFarbe, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }

    // Nullpunkt als Kreuz
    val arm = 14f * dichte
    drawLine(achsenFarbe, Offset(verschiebung.x - arm, verschiebung.y), Offset(verschiebung.x + arm, verschiebung.y), strokeWidth = 3f)
    drawLine(achsenFarbe, Offset(verschiebung.x, verschiebung.y - arm), Offset(verschiebung.x, verschiebung.y + arm), strokeWidth = 3f)
    drawCircle(achsenFarbe, radius = arm, center = verschiebung, style = Stroke(width = 2f))
}

/**
 * Lineale am oberen und linken Rand (nur im Bearbeitungsmodus). Sie zeigen Meter ab dem
 * festen Nullpunkt und passen ihre Teilung dem Zoom an. [pinsel] ist nur ein wiederverwendeter
 * Textpinsel.
 */
private fun DrawScope.zeichneLineale(
    zoom: Float,
    dichte: Float,
    massstab: Float,
    verschiebung: Offset,
    hintergrund: Color,
    linie: Color,
    textFarbe: Color,
    pinsel: android.graphics.Paint,
) {
    val rand = 4f * dichte // der Rahmen des Bearbeitungsmodus liegt innen am Rand
    val dicke = 22f * dichte
    val kante = rand + dicke
    val pixelProMeter = zoom * dichte * massstab
    val index = netzIndex(pixelProMeter, 56f * dichte)
    val teilung = netzTeilung(index)
    val kleinMeter = NETZ_METER[index] / teilung
    val klein = kleinMeter * pixelProMeter

    drawRect(hintergrund, Offset(rand, rand), Size(size.width - rand, dicke))
    drawRect(hintergrund, Offset(rand, kante), Size(dicke, size.height - kante))
    drawLine(linie, Offset(rand, kante), Offset(size.width, kante), strokeWidth = 1f)
    drawLine(linie, Offset(kante, rand), Offset(kante, size.height), strokeWidth = 1f)

    pinsel.color = textFarbe.toArgb()
    pinsel.textSize = 10f * dichte

    // Waagerecht
    var i = floor((kante - verschiebung.x) / klein).toInt()
    val letzteX = ceil((size.width - verschiebung.x) / klein).toInt()
    while (i <= letzteX) {
        val x = verschiebung.x + i * klein
        if (x >= kante) {
            val gross = i.mod(teilung) == 0
            val laenge = (if (gross) 10f else 5f) * dichte
            drawLine(linie, Offset(x, kante - laenge), Offset(x, kante), strokeWidth = 1f)
            if (gross) {
                val text = meterText(i * kleinMeter)
                drawIntoCanvas { it.nativeCanvas.drawText(text, x + 3f * dichte, rand + 11f * dichte, pinsel) }
            }
        }
        i++
    }
    // Senkrecht (Beschriftung liegt auf der Seite)
    var j = floor((kante - verschiebung.y) / klein).toInt()
    val letzteY = ceil((size.height - verschiebung.y) / klein).toInt()
    while (j <= letzteY) {
        val y = verschiebung.y + j * klein
        if (y >= kante) {
            val gross = j.mod(teilung) == 0
            val laenge = (if (gross) 10f else 5f) * dichte
            drawLine(linie, Offset(kante - laenge, y), Offset(kante, y), strokeWidth = 1f)
            if (gross) {
                val text = meterText(j * kleinMeter)
                drawIntoCanvas {
                    val c = it.nativeCanvas
                    c.save()
                    c.translate(rand + 11f * dichte, y - 3f * dichte)
                    c.rotate(-90f)
                    c.drawText(text, 0f, 0f, pinsel)
                    c.restore()
                }
            }
        }
        j++
    }
    // Ecke mit Einheit
    drawIntoCanvas { it.nativeCanvas.drawText("m", rand + 6f * dichte, rand + 15f * dichte, pinsel) }
}

/**
 * Ebenenbereich am rechten Rand: die schmale Leiste (ein Punkt je Ebene, Pfeile, Auge) und, wenn
 * das Auge angetippt wurde, direkt daneben die ausgeklappte Liste der Ebenen mit ihren Flächen.
 * [ebenen] ist von hinten nach vorn sortiert. Eine Ebene ist ein Behälter für mehrere Flächen.
 */
@Composable
private fun EbenenBereich(
    ebenen: List<Ebene>,
    flaechen: List<Flaeche>,
    gegenstaende: List<Gegenstand>,
    aktiveId: Long?,
    gewaehlteFlaecheId: Long?,
    gewaehlterGegenstandId: Long?,
    offen: Boolean,
    onSchalteOffen: () -> Unit,
    onWaehleEbene: (Long) -> Unit,
    onWaehleFlaeche: (Long) -> Unit,
    onWaehleGegenstand: (Long) -> Unit,
    verdecktZeigen: Boolean,
    onVerdecktSchalten: () -> Unit,
    onEbenePlatz: (Long, Int) -> Unit,
    onOrdneElemente: (Long, List<String>) -> Unit,
    onElementInEbene: (String, Long, List<String>) -> Unit,
    onNeueEbene: () -> Unit,
    onUmbenennen: (Long) -> Unit,
    onNachVorn: (Long) -> Unit,
    onNachHinten: (Long) -> Unit,
    onLoeschen: (Long) -> Unit,
    onSicht: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val hoechstens = maxHeight
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (offen) {
                EbenenListe(
                    ebenen = ebenen,
                    flaechen = flaechen,
                    gegenstaende = gegenstaende,
                    aktiveId = aktiveId,
                    gewaehlteFlaecheId = gewaehlteFlaecheId,
                    gewaehlterGegenstandId = gewaehlterGegenstandId,
                    onWaehleEbene = onWaehleEbene,
                    onWaehleFlaeche = onWaehleFlaeche,
                    onWaehleGegenstand = onWaehleGegenstand,
                    verdecktZeigen = verdecktZeigen,
                    onVerdecktSchalten = onVerdecktSchalten,
                    onEbenePlatz = onEbenePlatz,
                    onOrdneElemente = onOrdneElemente,
                    onElementInEbene = onElementInEbene,
                    onNeueEbene = onNeueEbene,
                    onUmbenennen = onUmbenennen,
                    onNachVorn = onNachVorn,
                    onNachHinten = onNachHinten,
                    onLoeschen = onLoeschen,
                    onSicht = onSicht,
                    modifier = Modifier.width(284.dp).heightIn(max = hoechstens),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Ebenenleiste(
                ebenen = ebenen,
                aktiveId = aktiveId,
                onWahl = onWaehleEbene,
                onListe = onSchalteOffen,
                maxHoehe = hoechstens,
            )
        }
    }
}

/**
 * Dezente Ebenenleiste: Pfeil nach vorn, ein Punkt je Ebene (ganz oben die vorderste), Pfeil nach
 * hinten, darunter das Auge für die Liste. Der Punkt der aktiven Ebene ist hervorgehoben,
 * ausgeblendete Ebenen sind nur ein Ring.
 */
@Composable
private fun Ebenenleiste(
    ebenen: List<Ebene>,
    aktiveId: Long?,
    onWahl: (Long) -> Unit,
    onListe: () -> Unit,
    maxHoehe: androidx.compose.ui.unit.Dp,
) {
    val anzahl = ebenen.size
    val index = ebenen.indexOfFirst { it.id == aktiveId }
    val ringFarbe = MaterialTheme.colorScheme.tertiary
    val punktFarbe = MaterialTheme.colorScheme.outline
    val aktivFarbe = MaterialTheme.colorScheme.primary
    val platz = ((maxHoehe - 136.dp) / 30.dp).toInt().coerceAtLeast(1)
    val sichtbar = minOf(anzahl, platz)
    // Passen nicht alle Punkte hin, wandert der Ausschnitt mit der aktiven Ebene mit.
    val oberster = when {
        anzahl <= platz -> anzahl - 1
        index >= 0 -> (index + platz / 2).coerceIn(platz - 1, anzahl - 1)
        else -> anzahl - 1
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { if (index in 0 until anzahl - 1) onWahl(ebenen[index + 1].id) },
                enabled = index in 0 until anzahl - 1,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pfeil_hoch),
                    contentDescription = stringResource(R.string.ebene_nach_vorn),
                )
            }
            for (i in oberster downTo (oberster - sichtbar + 1)) {
                val e = ebenen[i]
                val aktiv = e.id == aktiveId
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 30.dp)
                        .clickable { onWahl(e.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(26.dp)) {
                        val r = (if (aktiv) 8f else 6f) * density
                        if (e.sicht == 2) {
                            drawCircle(punktFarbe, radius = r, center = center, style = Stroke(width = 1.5f * density))
                        } else {
                            val grundFarbe = if (aktiv) aktivFarbe else punktFarbe
                            drawCircle(if (e.sicht == 1) grundFarbe.copy(alpha = 0.45f) else grundFarbe, radius = r, center = center)
                        }
                        if (aktiv) {
                            drawCircle(ringFarbe, radius = 11f * density, center = center, style = Stroke(width = 2.5f * density))
                        }
                    }
                }
            }
            IconButton(
                onClick = { if (index > 0) onWahl(ebenen[index - 1].id) },
                enabled = index > 0,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pfeil_runter),
                    contentDescription = stringResource(R.string.ebene_nach_hinten),
                )
            }
            IconButton(onClick = onListe, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_auge),
                    contentDescription = stringResource(R.string.ebenen_liste_oeffnen),
                )
            }
        }
    }
}

/** Griff zum Ziehen (drei Striche). Meldet die Fingerbewegung nach oben, damit die Zeile mitwandern kann. */
@Composable
private fun ZiehGriff(
    schluessel: String,
    onStart: () -> Unit,
    onZiehen: (Float) -> Unit,
    onEnde: () -> Unit,
) {
    val start = rememberUpdatedState(onStart)
    val ziehen = rememberUpdatedState(onZiehen)
    val ende = rememberUpdatedState(onEnde)
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 40.dp)
            .pointerInput(schluessel) {
                detectDragGestures(
                    onDragStart = { start.value() },
                    onDrag = { change, betrag ->
                        change.consume()
                        ziehen.value(betrag.y)
                    },
                    onDragEnd = { ende.value() },
                    onDragCancel = { ende.value() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "≡",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Die ausgeklappte Liste: eine Zeile je Ebene (vorderste zuerst) mit Name, Anzahl, Auge zum
 * Ausblenden und Menü. Ein Pfeil klappt die Ebene auf und zeigt ihre Flächen und Gegenstände.
 * Am Ziehgriff (≡) lassen sich Ebenen und, innerhalb einer Ebene, Flächen und Gegenstände
 * halten und verschieben; das ändert die Reihenfolge (oben in der Liste = weiter vorn).
 */
@Composable
private fun EbenenListe(
    ebenen: List<Ebene>,
    flaechen: List<Flaeche>,
    gegenstaende: List<Gegenstand>,
    aktiveId: Long?,
    gewaehlteFlaecheId: Long?,
    gewaehlterGegenstandId: Long?,
    onWaehleEbene: (Long) -> Unit,
    onWaehleFlaeche: (Long) -> Unit,
    onWaehleGegenstand: (Long) -> Unit,
    verdecktZeigen: Boolean,
    onVerdecktSchalten: () -> Unit,
    onEbenePlatz: (Long, Int) -> Unit,
    onOrdneElemente: (Long, List<String>) -> Unit,
    onElementInEbene: (String, Long, List<String>) -> Unit,
    onNeueEbene: () -> Unit,
    onUmbenennen: (Long) -> Unit,
    onNachVorn: (Long) -> Unit,
    onNachHinten: (Long) -> Unit,
    onLoeschen: (Long) -> Unit,
    onSicht: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aufgeklappt by remember { mutableStateOf(setOf<Long>()) }
    var menueFuer by remember { mutableStateOf<Long?>(null) }
    val vorneZuerst = ebenen.asReversed()
    val jeEbene = remember(flaechen) { flaechen.groupBy { it.ebeneId } }
    val jeEbeneG = remember(gegenstaende) { gegenstaende.groupBy { it.ebeneId } }
    val haptik = LocalHapticFeedback.current
    val ziehHintergrund = MaterialTheme.colorScheme.surfaceVariant

    // Ziehen: Lage jeder Zeile (oben, Höhe) und die laufende Bewegung der gezogenen Zeile
    val lagen = remember { mutableStateMapOf<String, Offset>() }
    var ziehKey by remember { mutableStateOf<String?>(null) }
    var ziehStart by remember { mutableFloatStateOf(0f) }
    var ziehDy by remember { mutableFloatStateOf(0f) }

    fun zeilenFuer(ebeneId: Long): List<ZeilenEintrag> =
        (
            jeEbene[ebeneId].orEmpty().map { ZeilenEintrag(it.reihenfolge, it, null) } +
                jeEbeneG[ebeneId].orEmpty().map { ZeilenEintrag(it.reihenfolge, null, it) }
            ).sortedByDescending { it.reihenfolge }

    fun schluesselVon(z: ZeilenEintrag): String = z.flaeche?.let { "f" + it.id } ?: ("g" + (z.gegenstand?.id ?: 0L))

    fun ebeneVon(key: String): Long? {
        val id = key.drop(1).toLongOrNull() ?: return null
        return if (key.startsWith("f")) {
            flaechen.firstOrNull { it.id == id }?.ebeneId
        } else {
            gegenstaende.firstOrNull { it.id == id }?.ebeneId
        }
    }

    fun mitteVon(key: String): Float {
        val l = lagen[key] ?: return 0f
        return l.x + l.y / 2f
    }

    /** Wie viele der anderen Zeilen liegen über der gezogenen Zeile (= neuer Platz von oben)? */
    fun platzVon(peers: List<String>, key: String): Int {
        val meineMitte = ziehStart + (lagen[key]?.y ?: 0f) / 2f + ziehDy
        return peers.filter { it != key }.count { mitteVon(it) < meineMitte }
    }

    /**
     * Wohin würde ein gezogenes Element fallen? Ebene (die unter der Mitte der gezogenen Zeile, sonst die nächste)
     * und Platz von oben in dieser Ebene (0 = ganz vorn). Bei einer eingeklappten fremden Ebene: ganz vorn.
     */
    fun elementZiel(key: String): Pair<Long, Int>? {
        if (key.startsWith("e")) return null
        val eigene = ebeneVon(key) ?: return null
        val meine = ziehStart + (lagen[key]?.y ?: 0f) / 2f + ziehDy
        var zielId: Long? = null
        var besterAbstand = Float.MAX_VALUE
        for (e in vorneZuerst) {
            val l = lagen["e" + e.id] ?: continue
            val abstand = if (meine < l.x) l.x - meine else if (meine > l.x + l.y) meine - (l.x + l.y) else 0f
            if (abstand < besterAbstand) {
                besterAbstand = abstand
                zielId = e.id
            }
        }
        val ziel = zielId ?: return null
        if (ziel != eigene && ziel !in aufgeklappt) return ziel to 0
        val peers = zeilenFuer(ziel).map { schluesselVon(it) }.filter { it != key }
        return ziel to peers.count { mitteVon(it) < meine }
    }

    fun startZiehen(key: String) {
        ziehKey = key
        ziehDy = 0f
        ziehStart = lagen[key]?.x ?: 0f
        haptik.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun endeZiehen() {
        val key = ziehKey ?: return
        if (key.startsWith("e")) {
            val id = key.drop(1).toLongOrNull()
            if (id != null) {
                val peers = vorneZuerst.map { "e" + it.id }
                val neuOben = platzVon(peers, key)
                val neuerPlatz = (ebenen.size - 1) - neuOben
                if (neuerPlatz != ebenen.indexOfFirst { it.id == id }) onEbenePlatz(id, neuerPlatz)
            }
        } else {
            val ebeneId = ebeneVon(key)
            val ziel = elementZiel(key)
            if (ebeneId != null && ziel != null) {
                val zielEbene = ziel.first
                val peers = zeilenFuer(zielEbene).map { schluesselVon(it) }
                val neu = peers.toMutableList()
                neu.remove(key)
                neu.add(ziel.second.coerceIn(0, neu.size), key)
                if (zielEbene == ebeneId) {
                    if (neu != peers) onOrdneElemente(ebeneId, neu.reversed())
                } else {
                    onElementInEbene(key, zielEbene, neu.reversed())
                    aufgeklappt = aufgeklappt + zielEbene
                }
            }
        }
        ziehKey = null
        ziehDy = 0f
    }

    fun Modifier.ziehbar(key: String): Modifier = this
        .onGloballyPositioned {
            // Während eines Ziehens bleiben die Lagen, wie sie vor dem Ziehen waren.
            val neu = Offset(it.positionInRoot().y, it.size.height.toFloat())
            if (ziehKey == null && lagen[key] != neu) lagen[key] = neu
        }
        .zIndex(if (ziehKey == key) 1f else 0f)
        .graphicsLayer {
            if (ziehKey == key) {
                // Die gehaltene Zeile hebt sich ab: größer, mit Schatten, folgt dem Finger.
                translationY = ziehDy
                scaleX = 1.04f
                scaleY = 1.04f
                shadowElevation = 18f
                shape = RoundedCornerShape(8.dp)
            }
        }
        .then(
            if (ziehKey == key) Modifier.background(ziehHintergrund, RoundedCornerShape(8.dp)) else Modifier,
        )

    // Ziel des laufenden Ziehens (für die Markierungen)
    val gezogen = ziehKey
    val elementZielJetzt: Pair<Long, Int>? = gezogen?.let { elementZiel(it) }
    val eigeneEbeneGezogen: Long? = gezogen?.let { if (it.startsWith("e")) null else ebeneVon(it) }
    val ebenenZielOben: Int? = if (gezogen != null && gezogen.startsWith("e")) {
        platzVon(vorneZuerst.map { "e" + it.id }, gezogen)
    } else {
        null
    }
    val linienFarbe = MaterialTheme.colorScheme.primary
    val zielFlaeche = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(6.dp)) {
            TextButton(onClick = onNeueEbene, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ebene_neu))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVerdecktSchalten() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.verdecktes_zeigen),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = verdecktZeigen, onCheckedChange = { onVerdecktSchalten() })
            }
            for (e in vorneZuerst) {
                val nummer = ebenen.indexOfFirst { it.id == e.id } + 1
                val anzahl = (jeEbene[e.id]?.size ?: 0) + (jeEbeneG[e.id]?.size ?: 0)
                val aktiv = e.id == aktiveId
                val offen = e.id in aufgeklappt
                val ebenenKey = "e" + e.id
                val ebenenIndexOhne = vorneZuerst.filter { "e" + it.id != gezogen }.indexOfFirst { it.id == e.id }
                val ebeneIstZiel = elementZielJetzt != null && elementZielJetzt.first == e.id && e.id != eigeneEbeneGezogen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ziehbar(ebenenKey)
                        .einfuegelinie(
                            oben = ebenenZielOben != null && ebenenIndexOhne >= 0 && ebenenZielOben == ebenenIndexOhne,
                            unten = ebenenZielOben != null && ebenenIndexOhne >= 0 &&
                                ebenenIndexOhne == vorneZuerst.size - 2 && ebenenZielOben == vorneZuerst.size - 1,
                            farbe = linienFarbe,
                        )
                        .then(
                            if (ebeneIstZiel) {
                                Modifier
                                    .background(zielFlaeche, RoundedCornerShape(10.dp))
                                    .border(2.dp, linienFarbe, RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (aktiv) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(8.dp),
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZiehGriff(
                        schluessel = ebenenKey,
                        onStart = { startZiehen(ebenenKey) },
                        onZiehen = { ziehDy += it },
                        onEnde = { endeZiehen() },
                    )
                    IconButton(
                        onClick = { aufgeklappt = if (offen) aufgeklappt - e.id else aufgeklappt + e.id },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pfeil_runter),
                            contentDescription = stringResource(R.string.ebene_aufklappen),
                            modifier = Modifier.rotate(if (offen) 0f else -90f),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onWaehleEbene(e.id) }
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = e.name ?: stringResource(R.string.ebene_nr, nummer),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (e.sicht == 2) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            } else {
                                Color.Unspecified
                            },
                        )
                        Text(
                            text = if (anzahl == 1) {
                                stringResource(R.string.element_eins)
                            } else {
                                stringResource(R.string.elemente_n, anzahl)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(
                        onClick = { onSicht(e.id, e.sicht) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                when (e.sicht) {
                                    1 -> R.drawable.ic_halb_verborgen
                                    2 -> R.drawable.ic_verborgen
                                    else -> R.drawable.ic_sichtbar
                                },
                            ),
                            contentDescription = stringResource(
                                when (e.sicht) {
                                    1 -> R.string.ebene_ausblenden
                                    2 -> R.string.ebene_einblenden
                                    else -> R.string.ebene_halbtransparent
                                },
                            ),
                        )
                    }
                    Box {
                        IconButton(onClick = { menueFuer = e.id }, modifier = Modifier.size(32.dp)) {
                            Text("⋮", style = MaterialTheme.typography.titleLarge)
                        }
                        DropdownMenu(expanded = menueFuer == e.id, onDismissRequest = { menueFuer = null }) {
                            MenuePunkt(R.string.ebene_umbenennen) {
                                menueFuer = null
                                onUmbenennen(e.id)
                            }
                            if (nummer < ebenen.size) {
                                MenuePunkt(R.string.ebene_vor) {
                                    menueFuer = null
                                    onNachVorn(e.id)
                                }
                            }
                            if (nummer > 1) {
                                MenuePunkt(R.string.ebene_zurueck) {
                                    menueFuer = null
                                    onNachHinten(e.id)
                                }
                            }
                            if (ebenen.size > 1) {
                                MenuePunkt(R.string.loeschen) {
                                    menueFuer = null
                                    onLoeschen(e.id)
                                }
                            }
                        }
                    }
                }
                if (offen) {
                    val zeilenListe = zeilenFuer(e.id)
                    val ohneGezogene = zeilenListe.filter { schluesselVon(it) != gezogen }
                    for (zeile in zeilenListe) {
                        val zeilenKey = schluesselVon(zeile)
                        val zeilenIndex = ohneGezogene.indexOfFirst { schluesselVon(it) == zeilenKey }
                        val zielHier = elementZielJetzt != null && elementZielJetzt.first == e.id && zeilenIndex >= 0
                        val g = zeile.gegenstand
                        val f = zeile.flaeche
                        val gewaehlt = (g != null && g.id == gewaehlterGegenstandId) ||
                            (f != null && f.id == gewaehlteFlaecheId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp)
                                .ziehbar(zeilenKey)
                                .einfuegelinie(
                                    oben = zielHier && elementZielJetzt?.second == zeilenIndex,
                                    unten = zielHier && zeilenIndex == ohneGezogene.size - 1 &&
                                        elementZielJetzt?.second == ohneGezogene.size,
                                    farbe = linienFarbe,
                                )
                                .background(
                                    if (gewaehlt) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable {
                                    if (g != null) onWaehleGegenstand(g.id) else if (f != null) onWaehleFlaeche(f.id)
                                }
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (g != null) {
                                val gart = GegenstandsArt.vonSchluessel(g.art)
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(g.farbe?.let { Color(it) } ?: gart.farbe, RoundedCornerShape(5.dp)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = g.name ?: stringResource(gart.nameRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            } else if (f != null) {
                                val art = Oberflaeche.vonSchluessel(f.oberflaeche)
                                Farbfeld(art, f.farbe)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = f.name ?: stringResource(art.nameRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            ZiehGriff(
                                schluessel = zeilenKey,
                                onStart = { startZiehen(zeilenKey) },
                                onZiehen = { ziehDy += it },
                                onEnde = { endeZiehen() },
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

/** Zeichnet beim Ziehen eine Linie über oder unter der Zeile, an der die gehaltene Zeile einrasten würde. */
private fun Modifier.einfuegelinie(oben: Boolean, unten: Boolean, farbe: Color): Modifier =
    if (!oben && !unten) {
        this
    } else {
        this.drawBehind {
            val dicke = 4.dp.toPx()
            val y = if (oben) -dicke / 2f else size.height - dicke / 2f
            drawRoundRect(farbe, topLeft = Offset(0f, y), size = Size(size.width, dicke), cornerRadius = CornerRadius(dicke / 2f))
        }
    }
