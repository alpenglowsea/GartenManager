package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Garten
import kotlinx.coroutines.delay
import kotlin.math.floor

private const val ZOOM_MIN = 0.1f
private const val ZOOM_MAX = 10f

/**
 * Geöffneter Garten: leere Zeichenfläche mit Zoomen und Verschieben (Teilschritt 2b).
 * Ansicht (Standard) und Bearbeiten unterscheiden sich durch Kopfzeile und Rahmen.
 *
 * Koordinaten: Bildschirmposition = Skizzenpunkt * zoom * dichte + verschiebung.
 * Die Verschiebung wird in dp gespeichert, damit sie unabhängig von der Bildschirmdichte ist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GartenScreen(
    garten: Garten,
    bearbeiten: Boolean,
    onBearbeiten: () -> Unit,
    onFertig: () -> Unit,
    onZurueck: () -> Unit,
    onAnsichtGeaendert: (zoom: Float, x: Float, y: Float) -> Unit,
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
                    .pointerInput(Unit) {
                        detectTransformGestures { schwerpunkt, verschieben, zoomFaktor, _ ->
                            val neuerZoom = (zoom * zoomFaktor).coerceIn(ZOOM_MIN, ZOOM_MAX)
                            val echterFaktor = neuerZoom / zoom
                            // Zoom um den Punkt zwischen den Fingern, damit der Inhalt dort stehen bleibt.
                            verschiebungX = schwerpunkt.x - (schwerpunkt.x - verschiebungX) * echterFaktor + verschieben.x
                            verschiebungY = schwerpunkt.y - (schwerpunkt.y - verschiebungY) * echterFaktor + verschieben.y
                            zoom = neuerZoom
                        }
                    },
            ) {
                zeichneGitter(zoom, dichte, Offset(verschiebungX, verschiebungY), gitterFarbe, achsenFarbe)
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

/**
 * Zeichnet ein Hilfsgitter, damit man Zoomen und Verschieben sieht, und den
 * Nullpunkt als Kreuz. Der Abstand der Gitterlinien springt in Zehnerschritten,
 * sodass die Linien auf dem Bildschirm nie zu dicht liegen.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.zeichneGitter(
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
