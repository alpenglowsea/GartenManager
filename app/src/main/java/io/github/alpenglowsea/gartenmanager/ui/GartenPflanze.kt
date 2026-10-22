package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.BildAusschnitt
import io.github.alpenglowsea.gartenmanager.daten.Gegenstand
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten
import io.github.alpenglowsea.gartenmanager.daten.GruppeMitAnzahl
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import io.github.alpenglowsea.gartenmanager.daten.PflanzeTreffer
import io.github.alpenglowsea.gartenmanager.daten.sucheMitEigenen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Pflanzen im Gartenplan (Teilschritt 4d1)
// ---------------------------------------------------------------------------------------------

/**
 * Üblicher Durchmesser einer ausgewachsenen Pflanze je Gruppe in Zentimetern. Das sind grobe
 * SCHÄTZWERTE als Startgröße, keine geprüften Pflanzendaten; man ändert die Größe nach dem Setzen.
 */
internal fun standardDurchmesserCm(gruppe: String): Float = when (gruppe) {
    "Stauden" -> 50f
    "Sträucher" -> 120f
    "Bäume" -> 400f
    "Heckenpflanzen" -> 80f
    "Gräser" -> 40f
    "Kletterpflanzen" -> 60f
    "Obstgehölze" -> 300f
    "Beerensträucher" -> 100f
    "Kräuter" -> 30f
    "Gemüsepflanzen" -> 40f
    "Blumen" -> 25f
    "Wasserpflanzen" -> 60f
    "Zwiebel- und Knollenblumen" -> 20f
    else -> 50f
}

internal fun istPflanze(g: Gegenstand): Boolean = g.art == io.github.alpenglowsea.gartenmanager.daten.ART_PFLANZE

/** Name, der zur Pflanze gezeigt wird: der eigene Name, sonst der Name aus den Grunddaten. */
internal fun pflanzenAnzeigename(g: Gegenstand): String? = g.name ?: g.pflanzeName

/** Ab dieser Kreisgröße (in dp auf dem Bildschirm) wird das Foto eingeblendet, voll sichtbar ab [FOTO_VOLL_DP]. */
internal const val FOTO_BEGINN_DP = 70f
internal const val FOTO_VOLL_DP = 110f

/** Ab dieser Kreisgröße (in dp) werden die Initialen gezeichnet. */
private const val INITIALEN_AB_DP = 40f

/** Kleinste Kantenlänge (kürzere Bildseite in Pixel), mit der Fotos für den Gartenplan geladen werden. */
internal const val FOTO_KANTE_GARTEN = 320

/**
 * Zeichnet eine gesetzte Pflanze als Kreis mit [durchmesser] Pixeln um [mitte]: Gruppenfarbe, Piktogramm und
 * Initialen. Wird der Kreis groß genug und gibt es ein [foto], blendet es sich ein (mit dem persönlichen
 * [ausschnitt] wie die Kachel im Katalog); ein Ring in der Gruppenfarbe bleibt.
 */
internal fun DrawScope.zeichneGartenPflanze(
    mitte: Offset,
    durchmesser: Float,
    farbe: Color,
    gruppe: String,
    namePerInitialen: String,
    dichte: Float,
    foto: ImageBitmap?,
    ausschnitt: BildAusschnitt?,
    pinsel: android.graphics.Paint,
    inDerHand: Boolean,
    umriss: Color?,
    umrissBreite: Float,
) {
    val grund = if (inDerHand) lerp(farbe, Color.White, 0.35f) else farbe
    val r = durchmesser / 2f
    val mitInitialen = durchmesser >= INITIALEN_AB_DP * dichte
    zeichnePflanzenKreis(mitte, durchmesser, grund, gruppe, mitInitialen)
    if (mitInitialen) {
        val text = initialen(namePerInitialen)
        if (text.isNotEmpty()) {
            pinsel.textSize = durchmesser * 0.2f
            pinsel.color = if (grund.luminance() > 0.45f) dunkler(grund, 0.3f).toArgb() else android.graphics.Color.WHITE
            val y = mitte.y + durchmesser * 0.31f - (pinsel.ascent() + pinsel.descent()) / 2f
            drawIntoCanvas { it.nativeCanvas.drawText(text, mitte.x, y, pinsel) }
        }
    }
    if (foto != null) {
        val dp = durchmesser / dichte
        val t = ((dp - FOTO_BEGINN_DP) / (FOTO_VOLL_DP - FOTO_BEGINN_DP)).coerceIn(0f, 1f)
        if (t > 0f) {
            val kreis = Path().apply { addOval(Rect(mitte.x - r, mitte.y - r, mitte.x + r, mitte.y + r)) }
            val bereich = ausschnittBereich(
                foto.width.toFloat(), foto.height.toFloat(), KACHEL_SEITEN,
                ausschnitt?.mitteX ?: 0.5f, ausschnitt?.mitteY ?: 0.5f, ausschnitt?.zoom ?: 1f,
            )
            clipPath(kreis) {
                drawImage(
                    image = foto,
                    srcOffset = IntOffset(bereich.left.roundToInt(), bereich.top.roundToInt()),
                    srcSize = IntSize(bereich.width.roundToInt().coerceAtLeast(1), bereich.height.roundToInt().coerceAtLeast(1)),
                    dstOffset = IntOffset((mitte.x - r).roundToInt(), (mitte.y - r).roundToInt()),
                    dstSize = IntSize(durchmesser.roundToInt().coerceAtLeast(1), durchmesser.roundToInt().coerceAtLeast(1)),
                    alpha = t,
                    filterQuality = FilterQuality.Medium,
                )
            }
            // Ring in der Gruppenfarbe, damit man die Gruppe auch am Foto erkennt
            val ring = durchmesser * 0.05f
            drawCircle(grund.copy(alpha = t), radius = r - ring / 2f, center = mitte, style = Stroke(width = ring))
        }
    }
    if (inDerHand) drawCircle(Color.White.copy(alpha = 0.2f), radius = r, center = mitte)
    if (umriss != null) drawCircle(umriss, radius = r, center = mitte, style = Stroke(width = umrissBreite))
}

/**
 * Auswahl einer Pflanze aus den Grunddaten (Suche und Gruppenfilter), als Fenster über dem Garten.
 * Ein Tipp auf einen Eintrag wählt die Pflanze.
 */
@Composable
internal fun PflanzenWahlDialog(onWahl: (PflanzeTreffer) -> Unit, onAbbrechen: () -> Unit) {
    val context = LocalContext.current
    var gruppen by remember { mutableStateOf<List<GruppeMitAnzahl>>(emptyList()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var suchtext by rememberSaveable { mutableStateOf("") }
    var gewaehlt by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var treffer by remember { mutableStateOf<List<PflanzeTreffer>>(emptyList()) }
    val dao = remember { MeineDatenDb.holen(context).gartenDao() }
    val ausschnitte by remember { dao.alleAusschnitte() }.collectAsState(initial = emptyList())
    val ausschnittNachId = remember(ausschnitte) { ausschnitte.associateBy { it.pflanzeId } }
    val eigenePflanzen by remember { dao.eigenePflanzen() }.collectAsState(initial = emptyList())
    var editorOffen by remember { mutableStateOf(false) }
    var nurEigene by remember { mutableStateOf(false) }
    // Kopf (Suche, Filter, Trefferzeile) wandert beim Scrollen mit und blendet aus; Hochscrollen holt ihn zurück.
    var kopfHoehe by remember { mutableStateOf(0) }
    var kopfVersatz by remember { mutableStateOf(0f) }
    val kopfVerbindung = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                kopfVersatz = (kopfVersatz + available.y).coerceIn(-kopfHoehe.toFloat(), 0f)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            gruppen = withContext(Dispatchers.IO) { Grunddaten.gruppen(context) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fehler = e.toString()
        }
    }
    LaunchedEffect(suchtext, gewaehlt, gruppen, eigenePflanzen, nurEigene) {
        try {
            treffer = withContext(Dispatchers.IO) { sucheMitEigenen(context, suchtext, gewaehlt, gruppen, nurEigene) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fehler = e.toString()
        }
    }

    if (editorOffen) {
        EigenePflanzeDialog(id = 0L, onFertig = { editorOffen = false })
    }

    Dialog(
        onDismissRequest = onAbbrechen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.pflanze_waehlen_titel),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onAbbrechen) {
                        Text(stringResource(R.string.abbrechen), maxLines = 1, softWrap = false)
                    }
                }
                val kopfDp = with(LocalDensity.current) { kopfHoehe.toDp() }
                Box(Modifier.fillMaxSize().nestedScroll(kopfVerbindung)) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = kopfDp)) {
                        items(treffer, key = { it.id }) { t ->
                            val farbe = gruppenFarbe(t.hauptgruppe)
                            val hintergrund = lerp(MaterialTheme.colorScheme.surface, farbe, 0.16f)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .height(IntrinsicSize.Min),
                            ) {
                                Box(Modifier.width(8.dp).fillMaxHeight().background(farbe))
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .background(hintergrund)
                                        .clickable { onWahl(t) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    PflanzenKachelBild(
                                        bild = t.bild,
                                        gruppe = t.hauptgruppe,
                                        name = t.hauptname,
                                        ausschnitt = ausschnittNachId[t.id],
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(t.hauptname, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            t.lateinisch,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic,
                                        )
                                        Text(
                                            if (t.eigene) t.hauptgruppe + " · " + stringResource(R.string.eigene_pflanze_marke) else t.hauptgruppe,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        Modifier
                            .onSizeChanged { kopfHoehe = it.height }
                            .graphicsLayer {
                                translationY = kopfVersatz
                                alpha = if (kopfHoehe > 0) (1f + kopfVersatz / kopfHoehe).coerceIn(0f, 1f) else 1f
                            }
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                    if (fehler != null) {
                        Text(
                            stringResource(R.string.pflanzen_test_fehler, fehler ?: ""),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    OutlinedTextField(
                        value = suchtext,
                        onValueChange = { suchtext = it },
                        label = { Text(stringResource(R.string.pflanzen_test_suche)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "eigene") {
                            FilterChip(
                                selected = nurEigene,
                                onClick = { nurEigene = !nurEigene },
                                label = { Text(stringResource(R.string.eigene_pflanzen_filter)) },
                            )
                        }
                        items(gruppen, key = { it.id }) { g ->
                            FilterChip(
                                selected = g.id in gewaehlt,
                                onClick = { gewaehlt = if (g.id in gewaehlt) gewaehlt - g.id else gewaehlt + g.id },
                                label = { Text("${g.name} (${g.anzahl})") },
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.pflanzen_test_treffer, treffer.size),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                        TextButton(onClick = { editorOffen = true }) { Text(stringResource(R.string.eigene_pflanze_anlegen)) }
                    }
                    HorizontalDivider()
                    }
                }
            }
        }
    }
}
