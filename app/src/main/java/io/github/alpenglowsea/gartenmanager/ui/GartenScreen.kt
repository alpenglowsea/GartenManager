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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.GartenViewModel
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Flaeche
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Punkt
import kotlinx.coroutines.delay
import kotlin.math.floor

private const val ZOOM_MIN = 0.1f
private const val ZOOM_MAX = 10f
private const val SCHLIESS_RADIUS_DP = 28f
private const val MIN_FORM_DP = 16f
private const val GRIFF_RADIUS_DP = 26f
private const val KANTE_RADIUS_DP = 18f

/** Ein laufendes Ziehen mit einem Finger: einen Punkt ([punktId]) oder die ganze Fläche (punktId = null). */
private data class Ziehen(
    val flaecheId: Long,
    val punktId: Long?,
    val start: Offset,
    val aktuell: Offset,
)

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
    val gewaehlteFlaeche = flaechen.firstOrNull { it.id == viewModel.auswahlFlaeche }
    val gewaehltePunkte = gewaehlteFlaeche?.let { punkteJeFlaeche[it.id] }.orEmpty()
    val gewaehlterPunkt = gewaehltePunkte.firstOrNull { it.id == viewModel.auswahlPunkt }

    // Zustand der Gesten
    var zeigerPos by remember { mutableStateOf<Offset?>(null) } // Finger beim Zeichnen/Aufziehen (Bildschirm)
    var formStart by remember { mutableStateOf<Offset?>(null) } // Startpunkt einer aufgezogenen Form (Skizze)
    var druckStart by remember { mutableStateOf<Offset?>(null) } // Fingerauflage im Auswahlmodus (Bildschirm)
    var bewegt by remember { mutableStateOf(false) }
    var ziehen by remember { mutableStateOf<Ziehen?>(null) }
    var ziehenFertig by remember { mutableStateOf(false) }

    // Dialoge
    var hilfeDialog by remember { mutableStateOf(false) }
    var formenDialog by remember { mutableStateOf(false) }
    var namenDialog by remember { mutableStateOf(false) }
    var loeschenDialog by remember { mutableStateOf(false) }

    val faktor = zoom * dichte
    fun zuSkizze(bild: Offset) = Offset((bild.x - verschiebungX) / faktor, (bild.y - verschiebungY) / faktor)
    fun zuBildschirm(skizze: Offset) = Offset(skizze.x * faktor + verschiebungX, skizze.y * faktor + verschiebungY)

    // Reichweite der Ausrichtungshilfe: 12 dp auf dem Bildschirm, umgerechnet in Skizzeneinheiten.
    val einrastSchwelle = 12f / zoom
    val zeichnet = zeichnung != null || formAuswahl != null

    // --- Vorschau des nächsten Punktes beim Zeichnen ---
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

    // --- Ergebnis eines laufenden Ziehens (Punkt mit Einrasten, oder Flächenverschiebung) ---
    val ziehDelta: Offset = ziehen?.let { it.aktuell - it.start } ?: Offset.Zero
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
                    rastePunktEin(roh, listOf(Offset(vor.x, vor.y), Offset(nach.x, nach.y)), einrastSchwelle, mitKante = false)
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

    // --- Tipp im Auswahlmodus ---
    fun beiTippAuswahl(pos: Offset) {
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
        // 3. Die oberste Fläche unter dem Finger auswählen, sonst Auswahl aufheben
        for (kandidat in flaechen.asReversed()) {
            val ihre = punkteJeFlaeche[kandidat.id] ?: continue
            if (ihre.size < 3) continue
            val poly = kurvenPolylinie(bildPunkte(kandidat, ihre), ihre.map { it.rund }, true)
            if (liegtInnen(poly, pos)) {
                viewModel.waehleFlaeche(kandidat.id)
                return
            }
        }
        viewModel.waehleFlaeche(null)
    }

    val beiZeiger = rememberUpdatedState<(Offset?) -> Unit> { pos ->
        if (pos == null) {
            // Geste abgebrochen oder beendet
            zeigerPos = null
            formStart = null
            druckStart = null
            if (!ziehenFertig) ziehen = null
        } else {
            zeigerPos = pos
            if (formAuswahl != null) {
                if (formStart == null) formStart = zuSkizze(pos)
            } else if (zeichnung == null && bearbeiten) {
                val start = druckStart
                if (start == null) {
                    // Finger ist gerade aufgesetzt worden: was liegt darunter?
                    druckStart = pos
                    bewegt = false
                    val fl = gewaehlteFlaeche
                    val griff = griffBei(pos)
                    ziehen = if (fl != null && griff != null) {
                        Ziehen(fl.id, griff.id, zuSkizze(pos), zuSkizze(pos))
                    } else if (fl != null && innerhalbGewaehlter(pos)) {
                        Ziehen(fl.id, null, zuSkizze(pos), zuSkizze(pos))
                    } else {
                        null
                    }
                } else {
                    if (!bewegt && (pos - start).getDistance() > touchSlop) bewegt = true
                    val z = ziehen
                    if (bewegt && z != null) ziehen = z.copy(aktuell = zuSkizze(pos))
                }
            }
        }
    }

    val beiLoslassen = rememberUpdatedState<(Offset) -> Unit> { pos ->
        if (formAuswahl != null) {
            val start = formStart
            if (start != null) {
                val punkteDerForm = formPunkte(formAuswahl, start, zuSkizze(pos), MIN_FORM_DP / faktor)
                if (punkteDerForm.isNotEmpty()) {
                    viewModel.legeFormAn(gartenId, punkteDerForm.map { it.lage }, punkteDerForm.map { it.rund })
                }
            }
        } else if (zeichnung != null) {
            if (nahErstemPunkt) {
                viewModel.schliesseFlaecheAb(gartenId)
            } else {
                val sk = zuSkizze(pos)
                viewModel.setzePunkt(if (einrasten) rastePunktEin(sk, zeichnung, einrastSchwelle).punkt else sk)
            }
        } else if (bearbeiten) {
            val z = ziehen
            if (z != null && bewegt) {
                val fertig = z.copy(aktuell = zuSkizze(pos))
                if (fertig.punktId != null) {
                    val ergebnis = ziehErgebnis
                    if (ergebnis != null) {
                        viewModel.verschiebePunkt(gartenId, fertig.punktId, ergebnis.punkt.x, ergebnis.punkt.y)
                    }
                } else {
                    val delta = fertig.aktuell - fertig.start
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
    LaunchedEffect(ziehenFertig, punkte) {
        if (ziehenFertig) {
            delay(150)
            ziehen = null
            ziehenFertig = false
        }
    }
    // Beim Wechsel der Betriebsart keine Reste einer Geste behalten.
    LaunchedEffect(bearbeiten, zeichnet) {
        zeigerPos = null
        formStart = null
        druckStart = null
        ziehen = null
        ziehenFertig = false
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
                    TextButton(onClick = if (bearbeiten) onFertig else onZurueck) {
                        Text(stringResource(R.string.zurueck))
                    }
                },
                actions = {
                    TextButton(onClick = { hilfeDialog = true }) { Text(stringResource(R.string.hilfe_knopf)) }
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
                    zeichnungRund = viewModel.zeichnungRund,
                    formAuswahl = formAuswahl,
                    einrasten = einrasten,
                    kannRueckgaengig = viewModel.anzahlRueckgaengig > 0,
                    gewaehlteFlaeche = gewaehlteFlaeche,
                    gewaehlterPunkt = gewaehlterPunkt,
                    punktLoeschenMoeglich = gewaehltePunkte.size > 3,
                    onFlaecheZeichnen = viewModel::starteZeichnung,
                    onFormenWaehlen = { formenDialog = true },
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
                    onNameAendern = { namenDialog = true },
                    onNachVorn = { gewaehlteFlaeche?.let { viewModel.bewegeFlaecheInReihenfolge(gartenId, it.id, 1) } },
                    onNachHinten = { gewaehlteFlaeche?.let { viewModel.bewegeFlaecheInReihenfolge(gartenId, it.id, -1) } },
                    onAlleRund = { gewaehlteFlaeche?.let { viewModel.setzeAlleRund(gartenId, it.id, true) } },
                    onAlleEckig = { gewaehlteFlaeche?.let { viewModel.setzeAlleRund(gartenId, it.id, false) } },
                    onFlaecheLoeschen = { loeschenDialog = true },
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
                            einFingerAktion = bearbeiten,
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
                zeichneGitter(zoom, dichte, Offset(verschiebungX, verschiebungY), gitterFarbe, achsenFarbe)

                // Fertige Flächen: die mit der höheren Reihenfolge liegen oben (sind schon sortiert).
                for (flaeche in flaechen) {
                    val ihre = punkteJeFlaeche[flaeche.id] ?: continue
                    if (ihre.size < 3) continue
                    val bild = bildPunkte(flaeche, ihre)
                    val pfad = kurvenPfad(bild, ihre.map { it.rund }, geschlossen = true)
                    drawPath(pfad, FLAECHE_FUELLUNG)
                    drawPath(
                        pfad,
                        FLAECHE_RAND,
                        style = Stroke(width = 2f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    if (bearbeiten && flaeche.id == gewaehlteFlaeche?.id) {
                        drawPath(
                            pfad,
                            rahmenFarbe,
                            style = Stroke(width = 3f * dichte, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
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
            onSchliessen = { hilfeDialog = false },
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

private val FLAECHE_FUELLUNG = Color(0xFF8BC34A).copy(alpha = 0.6f)
private val FLAECHE_RAND = Color(0xFF33691E)

@Composable
private fun Werkzeugleiste(
    zeichnung: List<Offset>?,
    zeichnungRund: Boolean,
    formAuswahl: Form?,
    einrasten: Boolean,
    kannRueckgaengig: Boolean,
    gewaehlteFlaeche: Flaeche?,
    gewaehlterPunkt: Punkt?,
    punktLoeschenMoeglich: Boolean,
    onFlaecheZeichnen: () -> Unit,
    onFormenWaehlen: () -> Unit,
    onRueckgaengig: () -> Unit,
    onAbbrechen: () -> Unit,
    onSchalteRund: () -> Unit,
    onSchalteEinrasten: () -> Unit,
    onAbwaehlen: () -> Unit,
    onPunktLoeschen: () -> Unit,
    onPunktRundSchalten: () -> Unit,
    onNameAendern: () -> Unit,
    onNachVorn: () -> Unit,
    onNachHinten: () -> Unit,
    onAlleRund: () -> Unit,
    onAlleEckig: () -> Unit,
    onFlaecheLoeschen: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (zeichnung != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAbbrechen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.abbrechen), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRueckgaengig,
                        enabled = zeichnung.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.rueckgaengig), maxLines = 1)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAbbrechen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.abbrechen), maxLines = 1)
                    }
                    OutlinedButton(onClick = onFormenWaehlen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.andere_form), maxLines = 1)
                    }
                }
            } else if (gewaehlteFlaeche != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (gewaehlterPunkt != null) {
                        OutlinedButton(
                            onClick = onPunktLoeschen,
                            enabled = punktLoeschenMoeglich,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.punkt_loeschen), maxLines = 1)
                        }
                        OutlinedButton(onClick = onPunktRundSchalten, modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(if (gewaehlterPunkt.rund) R.string.punkt_zu_ecke else R.string.punkt_zu_rund),
                                maxLines = 1,
                            )
                        }
                    } else {
                        var menueOffen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(onClick = { menueOffen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.flaeche_menue), maxLines = 1)
                            }
                            DropdownMenu(expanded = menueOffen, onDismissRequest = { menueOffen = false }) {
                                MenuePunkt(R.string.menue_name_aendern) { menueOffen = false; onNameAendern() }
                                MenuePunkt(R.string.menue_nach_vorn) { menueOffen = false; onNachVorn() }
                                MenuePunkt(R.string.menue_nach_hinten) { menueOffen = false; onNachHinten() }
                                MenuePunkt(R.string.alle_rund) { menueOffen = false; onAlleRund() }
                                MenuePunkt(R.string.alle_eckig) { menueOffen = false; onAlleEckig() }
                                MenuePunkt(if (einrasten) R.string.einrasten_an else R.string.einrasten_aus) {
                                    menueOffen = false
                                    onSchalteEinrasten()
                                }
                                MenuePunkt(R.string.loeschen) { menueOffen = false; onFlaecheLoeschen() }
                            }
                        }
                        OutlinedButton(onClick = onAbwaehlen, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.abwaehlen), maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = onRueckgaengig,
                        enabled = kannRueckgaengig,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.rueckgaengig), maxLines = 1)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onFlaecheZeichnen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.zeichnen), maxLines = 1)
                    }
                    Button(onClick = onFormenWaehlen, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.form_waehlen), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRueckgaengig,
                        enabled = kannRueckgaengig,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.rueckgaengig), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuePunkt(textId: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(textId)) }, onClick = onClick)
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
                    if (!zeichnet && !formModus && !flaecheGewaehlt) Text(stringResource(R.string.hilfe_werkzeuge))
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
