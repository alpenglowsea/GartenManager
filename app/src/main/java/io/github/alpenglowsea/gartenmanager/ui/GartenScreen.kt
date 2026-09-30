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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
    formAuswahl: Form?,
    einrasten: Boolean,
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
    onFormWaehlen: (Form) -> Unit,
    onFormAbbrechen: () -> Unit,
    onFormAnlegen: (punkte: List<Offset>, rund: List<Boolean>) -> Unit,
    onSchalteEinrasten: () -> Unit,
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

    // Finger beim Zeichnen oder Aufziehen einer Form (Bildschirmkoordinaten), sonst null.
    var zeigerPos by remember { mutableStateOf<Offset?>(null) }
    // Startpunkt einer aufgezogenen Form (Skizzenkoordinaten).
    var formStart by remember { mutableStateOf<Offset?>(null) }
    var formenDialog by remember { mutableStateOf(false) }

    val faktor = zoom * dichte
    fun zuSkizze(bild: Offset) = Offset((bild.x - verschiebungX) / faktor, (bild.y - verschiebungY) / faktor)
    fun zuBildschirm(skizze: Offset) = Offset(skizze.x * faktor + verschiebungX, skizze.y * faktor + verschiebungY)

    // Reichweite der Ausrichtungshilfe: 12 dp auf dem Bildschirm, umgerechnet in Skizzeneinheiten.
    val einrastSchwelle = 12f / zoom
    val aktion = bearbeiten && (zeichnung != null || formAuswahl != null)

    // Vorschau des nächsten Punktes (mit Ausrichtungshilfe), solange der Finger liegt.
    val vorschau: EinrastErgebnis? = run {
        val roh = zeigerPos
        if (zeichnung != null && formAuswahl == null && roh != null) {
            val sk = zuSkizze(roh)
            if (einrasten) rastePunktEin(sk, zeichnung, einrastSchwelle) else EinrastErgebnis(sk, emptyList())
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

    // Beim Wechsel der Betriebsart keine Reste einer Geste behalten.
    LaunchedEffect(aktion) {
        zeigerPos = null
        formStart = null
    }

    val beiZeiger = rememberUpdatedState<(Offset?) -> Unit> { pos ->
        zeigerPos = pos
        if (formAuswahl != null) {
            if (pos == null) formStart = null else if (formStart == null) formStart = zuSkizze(pos)
        }
    }
    val beiLoslassen = rememberUpdatedState<(Offset) -> Unit> { pos ->
        if (formAuswahl != null) {
            val start = formStart
            if (start != null) {
                val punkteDerForm = formPunkte(formAuswahl, start, zuSkizze(pos), MIN_FORM_DP / faktor)
                if (punkteDerForm.isNotEmpty()) {
                    onFormAnlegen(punkteDerForm.map { it.lage }, punkteDerForm.map { it.rund })
                }
            }
        } else if (zeichnung != null) {
            if (nahErstemPunkt) {
                onFlaecheAbschliessen()
            } else {
                val sk = zuSkizze(pos)
                onPunktSetzen(if (einrasten) rastePunktEin(sk, zeichnung, einrastSchwelle).punkt else sk)
            }
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
                    formAuswahl = formAuswahl,
                    einrasten = einrasten,
                    kannRueckgaengig = kannRueckgaengig,
                    onFlaecheZeichnen = onFlaecheZeichnen,
                    onFormenWaehlen = { formenDialog = true },
                    onRueckgaengig = if (zeichnung != null) onLetztenPunktEntfernen else onFlaecheRueckgaengig,
                    onAbbrechen = if (formAuswahl != null) onFormAbbrechen else onZeichnungAbbrechen,
                    onSchalteRund = onSchalteRund,
                    onSchalteEinrasten = onSchalteEinrasten,
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
                    .pointerInput(bearbeiten, aktion) {
                        gartenGesten(
                            einFingerVerschiebt = !bearbeiten,
                            einFingerAktion = aktion,
                            beiZeiger = { beiZeiger.value(it) },
                            beiLoslassen = { beiLoslassen.value(it) },
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

                // Angefangene Fläche: offene Linie durch die Punkte, erst ein Tipp auf den ersten Punkt schließt sie.
                if (zeichnung != null) {
                    val bild = zeichnung.map { zuBildschirm(it) }
                    val mitVorschau = if (vorschau != null) bild + zuBildschirm(vorschau.punkt) else bild
                    if (mitVorschau.size >= 2) {
                        val pfad = kurvenPfad(mitVorschau, List(mitVorschau.size) { zeichnungRund }, geschlossen = false)
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
                    val vorschauPunkte = formPunkte(formAuswahl, start, zuSkizze(ende), 0.001f)
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

    if (formenDialog) {
        FormenDialog(
            onWahl = { form ->
                formenDialog = false
                onFormWaehlen(form)
            },
            onAbbrechen = { formenDialog = false },
        )
    }
}

private const val SCHLIESS_RADIUS_DP = 28f
private const val MIN_FORM_DP = 16f

private val FLAECHE_FUELLUNG = Color(0xFF8BC34A).copy(alpha = 0.6f)
private val FLAECHE_RAND = Color(0xFF33691E)

@Composable
private fun Werkzeugleiste(
    zeichnung: List<Offset>?,
    zeichnungRund: Boolean,
    formAuswahl: Form?,
    einrasten: Boolean,
    kannRueckgaengig: Boolean,
    onFlaecheZeichnen: () -> Unit,
    onFormenWaehlen: () -> Unit,
    onRueckgaengig: () -> Unit,
    onAbbrechen: () -> Unit,
    onSchalteRund: () -> Unit,
    onSchalteEinrasten: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (zeichnung != null) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSchalteRund, modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(if (zeichnungRund) R.string.alle_eckig else R.string.alle_rund),
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(onClick = onSchalteEinrasten, modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(if (einrasten) R.string.einrasten_an else R.string.einrasten_aus),
                            maxLines = 1,
                        )
                    }
                }
            } else if (formAuswahl != null) {
                Text(
                    text = stringResource(R.string.form_hinweis, stringResource(formAuswahl.nameRes)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAbbrechen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.abbrechen))
                    }
                    OutlinedButton(onClick = onFormenWaehlen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.andere_form), maxLines = 1)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onFlaecheZeichnen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.flaeche_zeichnen), maxLines = 1)
                    }
                    Button(onClick = onFormenWaehlen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.form_waehlen), maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = onRueckgaengig,
                    enabled = kannRueckgaengig,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.rueckgaengig))
                }
            }
        }
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

/**
 * Eigene Gestenerkennung.
 *  - Zwei Finger zoomen und verschieben immer.
 *  - [einFingerVerschiebt]: ein Finger verschiebt die Ansicht (nur in der Ansicht).
 *  - [einFingerAktion]: ein Finger zeichnet oder zieht eine Form auf. Solange er liegt,
 *    meldet [beiZeiger] seine Position; beim Loslassen kommt [beiLoslassen]. Kommt ein
 *    zweiter Finger dazu, wird abgebrochen ([beiZeiger] mit null, kein [beiLoslassen]).
 */
private suspend fun PointerInputScope.gartenGesten(
    einFingerVerschiebt: Boolean,
    einFingerAktion: Boolean,
    beiZeiger: (Offset?) -> Unit,
    beiLoslassen: (Offset) -> Unit,
    beiTransform: (schwerpunkt: Offset, verschieben: Offset, zoomFaktor: Float) -> Unit,
) {
    awaitEachGesture {
        val runter = awaitFirstDown(requireUnconsumed = false)
        var maxFinger = 1
        var letztePosition = runter.position
        if (einFingerAktion) beiZeiger(runter.position)
        do {
            val ereignis = awaitPointerEvent()
            val fingerAnzahl = ereignis.changes.count { it.pressed }
            if (fingerAnzahl > maxFinger) maxFinger = fingerAnzahl
            if (fingerAnzahl >= 2) {
                if (einFingerAktion) beiZeiger(null)
                beiTransform(
                    ereignis.calculateCentroid(useCurrent = false),
                    ereignis.calculatePan(),
                    ereignis.calculateZoom(),
                )
                ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
            } else if (fingerAnzahl == 1) {
                val finger = ereignis.changes.first { it.pressed }
                letztePosition = finger.position
                if (einFingerAktion) {
                    if (maxFinger == 1) beiZeiger(finger.position)
                    ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                } else if (einFingerVerschiebt && (finger.position - runter.position).getDistance() > viewConfiguration.touchSlop) {
                    beiTransform(finger.position, ereignis.calculatePan(), 1f)
                    ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (ereignis.changes.any { it.pressed })
        if (einFingerAktion) {
            if (maxFinger == 1) beiLoslassen(letztePosition)
            beiZeiger(null)
        }
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
