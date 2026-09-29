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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Flaeche
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Punkt
import kotlinx.coroutines.delay
import kotlin.math.floor

private const val ZOOM_MIN = 0.1f
private const val ZOOM_MAX = 10f

/**
 * Geöffneter Garten: Zeichenfläche mit Zoomen und Verschieben (2b) und Flächen
 * zeichnen (2c). Ansicht (Standard) und Bearbeiten unterscheiden sich durch
 * Kopfzeile, Rahmen und Werkzeugleiste.
 *
 * Koordinaten: Bildschirmposition = Skizzenpunkt * zoom * dichte + verschiebung.
 * Die Verschiebung wird in dp gespeichert, damit sie unabhängig von der Bildschirmdichte ist.
 *
 * Bedienung: In der Ansicht verschiebt ein Finger, zwei Finger zoomen. Im Bearbeitungsmodus
 * ist ein Finger zum Punktesetzen frei, verschoben und gezoomt wird mit zwei Fingern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GartenScreen(
    garten: Garten,
    flaechen: List<Flaeche>,
    punkte: List<Punkt>,
    bearbeiten: Boolean,
    zeichnung: List<Offset>?,
    zeichnungRund: Boolean,
    kannRueckgaengig: Boolean,
    onBearbeiten: () -> Unit,
    onFertig: () -> Unit,
    onZurueck: () -> Unit,
    onAnsichtGeaendert: (zoom: Float, x: Float, y: Float) -> Unit,
    onFlaecheZeichnen: () -> Unit,
    onPunktSetzen: (Offset) -> Unit,
    onLetztenPunktEntfernen: () -> Unit,
    onFlaecheRueckgaengig: () -> Unit,
    onZeichnungAbbrechen: () -> Unit,
    onSchalteRund: () -> Unit,
    onFlaecheAbschliessen: () -> Unit,
) {
    val dichte = LocalDensity.current.density
    // Ansicht pro Garten neu aus den gespeicherten Werten starten (nur beim Öffnen).
    var zoom by remember(garten.id) { mutableFloatStateOf(garten.ansichtZoom) }
    var verschiebungX by remember(garten.id) { mutableFloatStateOf(garten.ansichtX * dichte) }
    var verschiebungY by remember(garten.id) { mutableFloatStateOf(garten.ansichtY * dichte) }

    // Ansicht kurz nach der letzten Bewegung speichern (nicht bei jedem Pixel).
    LaunchedEffect(garten.id, zoom, verschiebungX, verschiebungY) {
        delay(600)
        onAnsichtGeaendert(zoom, verschiebungX / dichte, verschiebungY / dichte)
    }

    val punkteJeFlaeche = remember(punkte) { punkte.groupBy { it.flaecheId } }

    // Ein Tipp auf die Fläche: im Zeichnen wird ein Punkt gesetzt (in Skizzenkoordinaten).
    val beiTipp = rememberUpdatedState<(Offset) -> Unit> { position ->
        if (bearbeiten && zeichnung != null) {
            val faktor = zoom * dichte
            onPunktSetzen(Offset((position.x - verschiebungX) / faktor, (position.y - verschiebungY) / faktor))
        }
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
                    )
                },
                navigationIcon = {
                    TextButton(onClick = if (bearbeiten) onFertig else onZurueck) {
                        Text(stringResource(R.string.zurueck))
                    }
                },
                actions = {
                    if (bearbeiten) {
                        TextButton(onClick = onFertig) { Text(stringResource(R.string.fertig)) }
                    } else {
                        TextButton(onClick = onBearbeiten) { Text(stringResource(R.string.bearbeiten)) }
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
                    zeichnungRund = zeichnungRund,
                    kannRueckgaengig = kannRueckgaengig,
                    onFlaecheZeichnen = onFlaecheZeichnen,
                    onRueckgaengig = if (zeichnung != null) onLetztenPunktEntfernen else onFlaecheRueckgaengig,
                    onAbbrechen = onZeichnungAbbrechen,
                    onSchalteRund = onSchalteRund,
                    onAbschliessen = onFlaecheAbschliessen,
                )
            }
        },
    ) { innerPadding ->
        val hintergrund = MaterialTheme.colorScheme.surface
        val gitterFarbe = MaterialTheme.colorScheme.outlineVariant
        val achsenFarbe = MaterialTheme.colorScheme.primary
        val rahmenFarbe = MaterialTheme.colorScheme.tertiary

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
                            beiTipp = { beiTipp.value(it) },
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
                val verschiebung = Offset(verschiebungX, verschiebungY)
                val faktor = zoom * dichte
                zeichneGitter(zoom, dichte, verschiebung, gitterFarbe, achsenFarbe)

                // Fertige Flächen: die mit der höheren Reihenfolge liegen oben (sind schon sortiert).
                for (flaeche in flaechen) {
                    val ihrePunkte = punkteJeFlaeche[flaeche.id] ?: continue
                    if (ihrePunkte.size < 3) continue
                    val bild = ihrePunkte.map { Offset(it.x * faktor + verschiebung.x, it.y * faktor + verschiebung.y) }
                    val pfad = kurvenPfad(bild, ihrePunkte.map { it.rund }, geschlossen = true)
                    drawPath(pfad, FLAECHE_FUELLUNG)
                    drawPath(
                        pfad,
                        FLAECHE_RAND,
                        style = Stroke(width = 2f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // Angefangene Fläche
                if (zeichnung != null && zeichnung.isNotEmpty()) {
                    val bild = zeichnung.map { Offset(it.x * faktor + verschiebung.x, it.y * faktor + verschiebung.y) }
                    val rund = List(bild.size) { zeichnungRund }
                    val geschlossen = bild.size >= 3
                    val pfad = kurvenPfad(bild, rund, geschlossen)
                    if (geschlossen) drawPath(pfad, rahmenFarbe.copy(alpha = 0.25f))
                    drawPath(
                        pfad,
                        rahmenFarbe,
                        style = Stroke(width = 3f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    bild.forEachIndexed { i, p ->
                        drawCircle(rahmenFarbe, radius = (if (i == 0) 9f else 6f) * dichte, center = p)
                    }
                }
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
}

private val FLAECHE_FUELLUNG = Color(0xFF8BC34A).copy(alpha = 0.6f)
private val FLAECHE_RAND = Color(0xFF33691E)

@Composable
private fun Werkzeugleiste(
    zeichnung: List<Offset>?,
    zeichnungRund: Boolean,
    kannRueckgaengig: Boolean,
    onFlaecheZeichnen: () -> Unit,
    onRueckgaengig: () -> Unit,
    onAbbrechen: () -> Unit,
    onSchalteRund: () -> Unit,
    onAbschliessen: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (zeichnung == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onFlaecheZeichnen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.flaeche_zeichnen))
                    }
                    OutlinedButton(
                        onClick = onRueckgaengig,
                        enabled = kannRueckgaengig,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.rueckgaengig))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.zeichnen_hinweis),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAbbrechen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.abbrechen))
                    }
                    OutlinedButton(
                        onClick = onRueckgaengig,
                        enabled = zeichnung.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.rueckgaengig))
                    }
                }
                OutlinedButton(onClick = onSchalteRund, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            if (zeichnungRund) R.string.alle_eckig else R.string.alle_rund,
                        ),
                    )
                }
                Button(
                    onClick = onAbschliessen,
                    enabled = zeichnung.size >= 3,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.flaeche_abschliessen))
                }
            }
        }
    }
}

/**
 * Eigene Gestenerkennung: ein Tipp (ein Finger, kaum bewegt) meldet [beiTipp];
 * zwei Finger zoomen und verschieben immer; ein Finger verschiebt nur, wenn
 * [einFingerVerschiebt] gilt (in der Ansicht, nicht beim Zeichnen).
 */
private suspend fun PointerInputScope.gartenGesten(
    einFingerVerschiebt: Boolean,
    beiTipp: (Offset) -> Unit,
    beiTransform: (schwerpunkt: Offset, verschieben: Offset, zoomFaktor: Float) -> Unit,
) {
    awaitEachGesture {
        val runter = awaitFirstDown(requireUnconsumed = false)
        val schwelle = viewConfiguration.touchSlop
        var maxFinger = 1
        var bewegt = false
        do {
            val ereignis = awaitPointerEvent()
            val fingerAnzahl = ereignis.changes.count { it.pressed }
            if (fingerAnzahl > maxFinger) maxFinger = fingerAnzahl
            if (fingerAnzahl >= 2) {
                bewegt = true
                beiTransform(
                    ereignis.calculateCentroid(useCurrent = false),
                    ereignis.calculatePan(),
                    ereignis.calculateZoom(),
                )
                ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
            } else if (fingerAnzahl == 1) {
                val finger = ereignis.changes.first { it.pressed }
                if (!bewegt && (finger.position - runter.position).getDistance() > schwelle) {
                    bewegt = true
                }
                if (bewegt && einFingerVerschiebt) {
                    beiTransform(finger.position, ereignis.calculatePan(), 1f)
                    ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (ereignis.changes.any { it.pressed })
        if (maxFinger == 1 && !bewegt) beiTipp(runter.position)
    }
}

/**
 * Kurve durch die Punkte (Catmull-Rom, als Bézier-Stücke). Ein Punkt mit rund = false
 * ist eine Ecke (dort keine weiche Führung). Die Führung wird auf 40 Prozent der Länge
 * des Teilstücks begrenzt, damit die Kurve bei sehr ungleichen Abständen nicht so
 * stark überschießt.
 */
private fun kurvenPfad(p: List<Offset>, rund: List<Boolean>, geschlossen: Boolean): Path {
    val pfad = Path()
    val n = p.size
    if (n == 0) return pfad
    pfad.moveTo(p[0].x, p[0].y)
    if (n == 1) return pfad
    val segmente = if (geschlossen) n else n - 1
    for (i in 0 until segmente) {
        val i2 = (i + 1) % n
        val p1 = p[i]
        val p2 = p[i2]
        val p0 = if (geschlossen) p[(i - 1 + n) % n] else p[maxOf(i - 1, 0)]
        val p3 = if (geschlossen) p[(i + 2) % n] else p[minOf(i + 2, n - 1)]
        val grenze = (p2 - p1).getDistance() * 0.4f
        val c1 = if (rund[i]) p1 + begrenze((p2 - p0) / 6f, grenze) else p1
        val c2 = if (rund[i2]) p2 - begrenze((p3 - p1) / 6f, grenze) else p2
        pfad.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    if (geschlossen) pfad.close()
    return pfad
}

private fun begrenze(v: Offset, maximum: Float): Offset {
    val laenge = v.getDistance()
    return if (laenge > maximum && laenge > 0f) v * (maximum / laenge) else v
}

/**
 * Zeichnet ein Hilfsgitter, damit man Zoomen und Verschieben sieht, und den
 * Nullpunkt als Kreuz. Der Abstand der Gitterlinien springt in Zehnerschritten,
 * sodass die Linien auf dem Bildschirm nie zu dicht liegen.
 */
private fun DrawScope.zeichneGitter(
    zoom: Float,
    dichte: Float,
    verschiebung: Offset,
    gitterFarbe: Color,
    achsenFarbe: Color,
) {
    val pixelProEinheit = zoom * dichte
    // Kleinster Abstand (100 * 10^n Einheiten), der auf dem Bildschirm mindestens ~48 dp beträgt.
    var abstand = 100f
    while (abstand * pixelProEinheit < 48f * dichte) abstand *= 10f
    while (abstand / 10f * pixelProEinheit >= 48f * dichte) abstand /= 10f
    val schritt = abstand * pixelProEinheit

    val ersteX = floor(-verschiebung.x / schritt).toInt()
    val letzteX = floor((size.width - verschiebung.x) / schritt).toInt() + 1
    for (i in ersteX..letzteX) {
        val x = verschiebung.x + i * schritt
        drawLine(gitterFarbe, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    }
    val ersteY = floor(-verschiebung.y / schritt).toInt()
    val letzteY = floor((size.height - verschiebung.y) / schritt).toInt() + 1
    for (i in ersteY..letzteY) {
        val y = verschiebung.y + i * schritt
        drawLine(gitterFarbe, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }

    // Nullpunkt als Kreuz
    val arm = 14f * dichte
    drawLine(achsenFarbe, Offset(verschiebung.x - arm, verschiebung.y), Offset(verschiebung.x + arm, verschiebung.y), strokeWidth = 3f)
    drawLine(achsenFarbe, Offset(verschiebung.x, verschiebung.y - arm), Offset(verschiebung.x, verschiebung.y + arm), strokeWidth = 3f)
    drawCircle(achsenFarbe, radius = arm, center = verschiebung, style = Stroke(width = 2f))
}
