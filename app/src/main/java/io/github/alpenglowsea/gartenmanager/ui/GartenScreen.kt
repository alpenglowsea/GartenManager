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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    ebenen: List<Ebene>,
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
    // Aktive Ebene: die der ausgewählten Fläche, sonst die zuletzt gewählte, sonst die vorderste.
    val aktiveEbeneId: Long? = gewaehlteFlaeche?.ebeneId
        ?: viewModel.aktiveEbeneId?.takeIf { id -> ebenen.any { it.id == id } }
        ?: ebenen.lastOrNull()?.id
    LaunchedEffect(gewaehlteFlaeche?.id, gewaehlteFlaeche?.ebeneId) {
        gewaehlteFlaeche?.let { viewModel.setzeAktiveEbene(it.ebeneId) }
    }

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
    // Flächenmenü (langer Tipp auf eine Fläche): Stelle des Fingers, sonst null
    var flaechenMenue by remember { mutableStateOf<Offset?>(null) }
    var oberflaecheDialog by remember { mutableStateOf(false) }
    // In der Ansicht angetippte Fläche (zeigt Name und Oberfläche)
    var infoFlaecheId by remember { mutableStateOf<Long?>(null) }
    // Liste aller Ebenen (Auge in der Ebenenleiste)
    var ebenenOffen by remember { mutableStateOf(false) }
    var ebeneUmbenennenId by remember { mutableStateOf<Long?>(null) }
    var ebeneLoeschenId by remember { mutableStateOf<Long?>(null) }
    val linealPinsel = remember { android.graphics.Paint().apply { isAntiAlias = true } }

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
        viewModel.waehleFlaeche(flaecheBei(pos)?.id)
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
                    if (!bewegt && (pos - start).getDistance() > touchSlop) {
                        bewegt = true
                        // Fläche "in der Hand": kurzes Fühlen, dazu Schatten und Rahmen in der Anzeige
                        if (ziehen != null && ziehen?.punktId == null) {
                            haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    val z = ziehen
                    if (bewegt && z != null) ziehen = z.copy(aktuell = zuSkizze(pos))
                }
            }
        }
    }

    val beiTippAnsicht = rememberUpdatedState<(Offset) -> Unit> { pos ->
        infoFlaecheId = if (bearbeiten) null else flaecheBei(pos)?.id
    }

    val beiLangemDruck = rememberUpdatedState<(Offset) -> Unit> { pos ->
        if (bearbeiten && zeichnung == null && formAuswahl == null) {
            val treffer = flaecheBei(pos)
            if (treffer != null) {
                ziehen = null
                viewModel.waehleFlaeche(treffer.id)
                haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                flaechenMenue = pos
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
        infoFlaecheId = null
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
                zeichneGitter(zoom, dichte, garten.massstab, Offset(verschiebungX, verschiebungY), gitterFarbe, achsenFarbe)

                // Fertige Flächen: die mit der höheren Reihenfolge liegen oben (sind schon sortiert).
                for (flaeche in sichtbareFlaechen) {
                    val ihre = punkteJeFlaeche[flaeche.id] ?: continue
                    if (ihre.size < 3) continue
                    val bild = bildPunkte(flaeche, ihre)
                    val pfad = kurvenPfad(bild, ihre.map { it.rund }, geschlossen = true)
                    val blass = flaeche.ebeneId in blasseEbenen
                    if (blass) drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = 0.35f })
                    val inDerHand = bewegt && ziehen?.punktId == null && ziehen?.flaecheId == flaeche.id
                    val art = Oberflaeche.vonSchluessel(flaeche.oberflaeche)
                    // Angefasste Fläche: Farbe heller und greller, damit man sieht, was man in der Hand hat
                    drawPath(pfad, if (inDerHand) lerp(art.fuellung, Color.White, 0.45f) else art.fuellung)
                    zeichneMuster(art, pfad, bild, faktor, dichte)
                    if (inDerHand) drawPath(pfad, Color.White.copy(alpha = 0.2f))
                    drawPath(
                        pfad,
                        art.rand,
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
                            Farbfeld(art)
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
                    aktiveId = aktiveEbeneId,
                    gewaehlteFlaecheId = viewModel.auswahlFlaeche,
                    offen = ebenenOffen,
                    onSchalteOffen = { ebenenOffen = !ebenenOffen },
                    onWaehleEbene = { viewModel.waehleEbene(it) },
                    onWaehleFlaeche = { viewModel.waehleFlaeche(it) },
                    onNeueEbene = { viewModel.legeEbeneAn(gartenId) },
                    onUmbenennen = { ebeneUmbenennenId = it },
                    onNachVorn = { viewModel.bewegeEbene(gartenId, it, 1) },
                    onNachHinten = { viewModel.bewegeEbene(gartenId, it, -1) },
                    onLoeschen = { id ->
                        // Eine leere Ebene wird gleich gelöscht, sonst wird gefragt, was mit dem Inhalt geschieht.
                        if (flaechen.none { it.ebeneId == id }) {
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
        val anzahlDrin = flaechen.count { it.ebeneId == ebeneLoeschen.id }
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

private val KNOPF_ABSTAND = PaddingValues(horizontal = 6.dp, vertical = 8.dp)

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnopfVoll(R.string.zeichnen, onFlaecheZeichnen, Modifier.weight(1f))
                    KnopfVoll(R.string.form_waehlen, onFormenWaehlen, Modifier.weight(1f))
                    KnopfUmriss(R.string.rueckgaengig, onRueckgaengig, Modifier.weight(1f), enabled = kannRueckgaengig)
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
    aktiveId: Long?,
    gewaehlteFlaecheId: Long?,
    offen: Boolean,
    onSchalteOffen: () -> Unit,
    onWaehleEbene: (Long) -> Unit,
    onWaehleFlaeche: (Long) -> Unit,
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
                    aktiveId = aktiveId,
                    gewaehlteFlaecheId = gewaehlteFlaecheId,
                    onWaehleEbene = onWaehleEbene,
                    onWaehleFlaeche = onWaehleFlaeche,
                    onNeueEbene = onNeueEbene,
                    onUmbenennen = onUmbenennen,
                    onNachVorn = onNachVorn,
                    onNachHinten = onNachHinten,
                    onLoeschen = onLoeschen,
                    onSicht = onSicht,
                    modifier = Modifier.width(260.dp).heightIn(max = hoechstens),
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

/**
 * Die ausgeklappte Liste: eine Zeile je Ebene (vorderste zuerst) mit Name, Anzahl, Auge zum
 * Ausblenden und Menü. Ein Pfeil klappt die Ebene auf und zeigt ihre Flächen.
 */
@Composable
private fun EbenenListe(
    ebenen: List<Ebene>,
    flaechen: List<Flaeche>,
    aktiveId: Long?,
    gewaehlteFlaecheId: Long?,
    onWaehleEbene: (Long) -> Unit,
    onWaehleFlaeche: (Long) -> Unit,
    onNeueEbene: () -> Unit,
    onUmbenennen: (Long) -> Unit,
    onNachVorn: (Long) -> Unit,
    onNachHinten: (Long) -> Unit,
    onLoeschen: (Long) -> Unit,
    onAusgeblendet: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aufgeklappt by remember { mutableStateOf(setOf<Long>()) }
    var menueFuer by remember { mutableStateOf<Long?>(null) }
    val vorneZuerst = ebenen.asReversed()
    val jeEbene = remember(flaechen) { flaechen.groupBy { it.ebeneId } }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        LazyColumn(contentPadding = PaddingValues(6.dp)) {
            item(key = "neu") {
                TextButton(onClick = onNeueEbene, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ebene_neu))
                }
            }
            for (e in vorneZuerst) {
                item(key = "e" + e.id) {
                    val nummer = ebenen.indexOfFirst { it.id == e.id } + 1
                    val anzahl = jeEbene[e.id]?.size ?: 0
                    val aktiv = e.id == aktiveId
                    val offen = e.id in aufgeklappt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (aktiv) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { aufgeklappt = if (offen) aufgeklappt - e.id else aufgeklappt + e.id },
                            modifier = Modifier.size(36.dp),
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
                            modifier = Modifier.size(36.dp),
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
                            IconButton(onClick = { menueFuer = e.id }, modifier = Modifier.size(36.dp)) {
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
                }
                if (e.id in aufgeklappt) {
                    val ihre = jeEbene[e.id].orEmpty().sortedByDescending { it.reihenfolge }
                    items(ihre, key = { "f" + it.id }) { f ->
                        val art = Oberflaeche.vonSchluessel(f.oberflaeche)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 36.dp)
                                .background(
                                    if (f.id == gewaehlteFlaecheId) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { onWaehleFlaeche(f.id) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Farbfeld(art)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = f.name ?: stringResource(art.nameRes),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
