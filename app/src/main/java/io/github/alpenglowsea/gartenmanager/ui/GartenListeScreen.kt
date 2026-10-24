package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.key
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Grundstueck
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Welcher Dialog gerade offen ist. */
private sealed interface DialogZustand {
    data object Keiner : DialogZustand
    data class NeuerGarten(val grundstueckId: Long?) : DialogZustand
    data object NeuesGrundstueck : DialogZustand
    data class GartenUmbenennen(val garten: Garten) : DialogZustand
    data class GrundstueckUmbenennen(val grundstueck: Grundstueck) : DialogZustand
    data class GartenVerschieben(val garten: Garten) : DialogZustand
    data class GartenLoeschen(val garten: Garten) : DialogZustand
    data class GrundstueckLoeschen(val grundstueck: Grundstueck, val anzahlGaerten: Int) : DialogZustand
}

/** Für welches Element gerade das Menü (langes Drücken oder Punkte-Knopf) offen ist. */
private sealed interface MenueZiel {
    data class ImGarten(val id: Long) : MenueZiel
    data class ImGrundstueck(val id: Long) : MenueZiel
}

private val DATUMS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun formatiere(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATUMS_FORMAT)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GartenListeScreen(
    grundstuecke: List<Grundstueck>?,
    gaerten: List<Garten>?,
    aufgeklappt: Set<Long>,
    pflanzenZahl: Int,
    artenZahl: Int,
    onSchalteAufgeklappt: (Long) -> Unit,
    onGartenOeffnen: (Long) -> Unit,
    onGartenBearbeiten: (Long) -> Unit,
    onGartenAnlegen: (name: String, grundstueckId: Long?) -> Unit,
    onGrundstueckAnlegen: (String) -> Unit,
    onGartenUmbenennen: (Long, String) -> Unit,
    onGrundstueckUmbenennen: (Long, String) -> Unit,
    onGartenVerschieben: (Long, Long?) -> Unit,
    onGartenDuplizieren: (Long, String) -> Unit,
    onGartenLoeschen: (Long) -> Unit,
    onGrundstueckAufloesen: (Long) -> Unit,
    onGrundstueckMitGaertenLoeschen: (Long) -> Unit,
    onGaertenOrdnen: (List<Long>) -> Unit,
    onKatalog: () -> Unit,
    onMeinePflanzen: () -> Unit,
    onUeber: () -> Unit,
    onOptionen: () -> Unit,
) {
    var dialog by remember { mutableStateOf<DialogZustand>(DialogZustand.Keiner) }
    var menue by remember { mutableStateOf<MenueZiel?>(null) }
    var fabMenue by remember { mutableStateOf(false) }
    var ueberMenue by remember { mutableStateOf(false) }
    val kopieZusatz = stringResource(R.string.kopie_zusatz)

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenue = true }) {
                    Text(text = "+", style = MaterialTheme.typography.headlineMedium)
                }
                DropdownMenu(expanded = fabMenue, onDismissRequest = { fabMenue = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.neuer_garten)) },
                        onClick = {
                            fabMenue = false
                            dialog = DialogZustand.NeuerGarten(null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.neues_grundstueck)) },
                        onClick = {
                            fabMenue = false
                            dialog = DialogZustand.NeuesGrundstueck
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (grundstuecke == null || gaerten == null) {
                // Wird geladen: kurz nichts anzeigen, damit der Leer-Hinweis nicht aufblitzt.
            } else {
                val einzelne = gaerten.filter { it.grundstueckId == null }
                val gartenKarte: @Composable (Garten, Color, Modifier) -> Unit = { garten, hintergrund, mod ->
                    GartenKarte(
                        garten = garten,
                        hintergrund = hintergrund,
                        modifier = mod,
                        menueOffen = menue == MenueZiel.ImGarten(garten.id),
                        onOeffnen = { onGartenOeffnen(garten.id) },
                        onMenueOeffnen = { menue = MenueZiel.ImGarten(garten.id) },
                        onMenueSchliessen = { menue = null },
                        onBearbeiten = {
                            menue = null
                            onGartenBearbeiten(garten.id)
                        },
                        onDuplizieren = {
                            menue = null
                            onGartenDuplizieren(garten.id, kopieZusatz)
                        },
                        onUmbenennen = {
                            menue = null
                            dialog = DialogZustand.GartenUmbenennen(garten)
                        },
                        onVerschieben = {
                            menue = null
                            dialog = DialogZustand.GartenVerschieben(garten)
                        },
                        onLoeschen = {
                            menue = null
                            dialog = DialogZustand.GartenLoeschen(garten)
                        },
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Kopf mit Name der App und Menü
                    item(key = "kopf") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Box {
                                IconButton(onClick = { ueberMenue = true }) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                                DropdownMenu(expanded = ueberMenue, onDismissRequest = { ueberMenue = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.opt_titel)) },
                                        onClick = {
                                            ueberMenue = false
                                            onOptionen()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ueber_titel)) },
                                        onClick = {
                                            ueberMenue = false
                                            onUeber()
                                        },
                                    )
                                }
                            }
                        }
                    }
                    item(key = "titel-gaerten") { Abschnittsname(stringResource(R.string.start_meine_gaerten)) }
                    if (grundstuecke.isEmpty() && gaerten.isEmpty()) {
                        item(key = "leer") {
                            LeerHinweis(onErstenGartenAnlegen = { dialog = DialogZustand.NeuerGarten(null) })
                        }
                    }
                    for (grundstueck in grundstuecke) {
                        val seineGaerten = gaerten.filter { it.grundstueckId == grundstueck.id }
                        val offen = grundstueck.id in aufgeklappt
                        item(key = "grundstueck-${grundstueck.id}") {
                            GrundstueckBlock(
                                grundstueck = grundstueck,
                                anzahlGaerten = seineGaerten.size,
                                offen = offen,
                                farbe = grundstueckFarbe(grundstueck.id),
                                menueOffen = menue == MenueZiel.ImGrundstueck(grundstueck.id),
                                onKlick = { onSchalteAufgeklappt(grundstueck.id) },
                                onMenueOeffnen = { menue = MenueZiel.ImGrundstueck(grundstueck.id) },
                                onMenueSchliessen = { menue = null },
                                onUmbenennen = {
                                    menue = null
                                    dialog = DialogZustand.GrundstueckUmbenennen(grundstueck)
                                },
                                onLoeschen = {
                                    menue = null
                                    dialog = DialogZustand.GrundstueckLoeschen(grundstueck, seineGaerten.size)
                                },
                            ) {
                                GartenReihe(
                                    gaerten = seineGaerten,
                                    seitenRand = 12.dp,
                                    hintergrund = MaterialTheme.colorScheme.surface,
                                    karte = { g, mod -> gartenKarte(g, MaterialTheme.colorScheme.surface, mod) },
                                    onNeu = { dialog = DialogZustand.NeuerGarten(grundstueck.id) },
                                    onOrdnen = onGaertenOrdnen,
                                )
                            }
                        }
                    }
                    if (grundstuecke.isNotEmpty() && einzelne.isNotEmpty()) {
                        item(key = "trenner") { Abschnittsname(stringResource(R.string.einzelne_gaerten)) }
                    }
                    if (einzelne.isNotEmpty()) {
                        item(key = "einzelne") {
                            GartenReihe(
                                gaerten = einzelne,
                                seitenRand = 16.dp,
                                hintergrund = MaterialTheme.colorScheme.surfaceContainerHigh,
                                karte = { g, mod -> gartenKarte(g, MaterialTheme.colorScheme.surfaceContainerHigh, mod) },
                                onNeu = null,
                                onOrdnen = onGaertenOrdnen,
                            )
                        }
                    }
                    // Kacheln für Pflanzen und Pflege
                    item(key = "titel-pflanzen") { Abschnittsname(stringResource(R.string.start_pflanzen_pflege)) }
                    item(key = "kacheln") {
                        val res = LocalContext.current.resources
                        val meinePflanzenText = if (pflanzenZahl == 0) {
                            stringResource(R.string.start_pflanzen_leer)
                        } else {
                            res.getQuantityString(R.plurals.n_pflanzen, pflanzenZahl, pflanzenZahl) + " · " +
                                res.getQuantityString(R.plurals.n_arten, artenZahl, artenZahl)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Kachel(
                                titel = stringResource(R.string.mp_titel),
                                text = meinePflanzenText,
                                farbe = MaterialTheme.colorScheme.secondaryContainer,
                                inhaltFarbe = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = onMeinePflanzen,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Kachel(
                                    titel = stringResource(R.string.katalog_menue),
                                    text = stringResource(R.string.start_katalog_text),
                                    farbe = MaterialTheme.colorScheme.primaryContainer,
                                    inhaltFarbe = MaterialTheme.colorScheme.onPrimaryContainer,
                                    onClick = onKatalog,
                                    modifier = Modifier.weight(1f),
                                )
                                Kachel(
                                    titel = stringResource(R.string.start_kalender),
                                    text = stringResource(R.string.start_kalender_bald),
                                    farbe = MaterialTheme.colorScheme.tertiaryContainer,
                                    inhaltFarbe = MaterialTheme.colorScheme.onTertiaryContainer,
                                    onClick = null,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    // Platz unter der letzten Zeile, damit der Plus-Knopf nichts verdeckt.
                    item(key = "platzhalter-unten") { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }
    }

    // ---- Dialoge ----
    when (val d = dialog) {
        DialogZustand.Keiner -> Unit
        is DialogZustand.NeuerGarten -> NameDialog(
            titel = stringResource(R.string.neuer_garten),
            startwert = "",
            bestaetigenText = stringResource(R.string.anlegen),
            onBestaetigt = {
                onGartenAnlegen(it, d.grundstueckId)
                dialog = DialogZustand.Keiner
            },
            onAbbruch = { dialog = DialogZustand.Keiner },
        )
        DialogZustand.NeuesGrundstueck -> NameDialog(
            titel = stringResource(R.string.neues_grundstueck),
            startwert = "",
            bestaetigenText = stringResource(R.string.anlegen),
            onBestaetigt = {
                onGrundstueckAnlegen(it)
                dialog = DialogZustand.Keiner
            },
            onAbbruch = { dialog = DialogZustand.Keiner },
        )
        is DialogZustand.GartenUmbenennen -> NameDialog(
            titel = stringResource(R.string.umbenennen),
            startwert = d.garten.name,
            bestaetigenText = stringResource(R.string.speichern),
            onBestaetigt = {
                onGartenUmbenennen(d.garten.id, it)
                dialog = DialogZustand.Keiner
            },
            onAbbruch = { dialog = DialogZustand.Keiner },
        )
        is DialogZustand.GrundstueckUmbenennen -> NameDialog(
            titel = stringResource(R.string.umbenennen),
            startwert = d.grundstueck.name,
            bestaetigenText = stringResource(R.string.speichern),
            onBestaetigt = {
                onGrundstueckUmbenennen(d.grundstueck.id, it)
                dialog = DialogZustand.Keiner
            },
            onAbbruch = { dialog = DialogZustand.Keiner },
        )
        is DialogZustand.GartenVerschieben -> VerschiebenDialog(
            aktuellesGrundstueckId = d.garten.grundstueckId,
            grundstuecke = grundstuecke.orEmpty(),
            onWahl = {
                onGartenVerschieben(d.garten.id, it)
                dialog = DialogZustand.Keiner
            },
            onAbbruch = { dialog = DialogZustand.Keiner },
        )
        is DialogZustand.GartenLoeschen -> BestaetigenDialog(
            titel = stringResource(R.string.garten_loeschen_titel, d.garten.name),
            text = stringResource(R.string.garten_loeschen_text),
            bestaetigenText = stringResource(R.string.loeschen),
            onBestaetigt = {
                onGartenLoeschen(d.garten.id)
                dialog = DialogZustand.Keiner
            },
            onAbbruch = { dialog = DialogZustand.Keiner },
        )
        is DialogZustand.GrundstueckLoeschen -> {
            if (d.anzahlGaerten == 0) {
                BestaetigenDialog(
                    titel = stringResource(R.string.grundstueck_loeschen_titel, d.grundstueck.name),
                    text = stringResource(R.string.grundstueck_leer_loeschen_text),
                    bestaetigenText = stringResource(R.string.loeschen),
                    onBestaetigt = {
                        onGrundstueckAufloesen(d.grundstueck.id)
                        dialog = DialogZustand.Keiner
                    },
                    onAbbruch = { dialog = DialogZustand.Keiner },
                )
            } else {
                GrundstueckLoeschenDialog(
                    name = d.grundstueck.name,
                    onAufloesen = {
                        onGrundstueckAufloesen(d.grundstueck.id)
                        dialog = DialogZustand.Keiner
                    },
                    onMitGaertenLoeschen = {
                        onGrundstueckMitGaertenLoeschen(d.grundstueck.id)
                        dialog = DialogZustand.Keiner
                    },
                    onAbbruch = { dialog = DialogZustand.Keiner },
                )
            }
        }
    }
}

@Composable
private fun Abschnittsname(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
    )
}

/** Große Kachel auf der Startseite. Ohne [onClick] ist sie abgeblendet (Funktion kommt später). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Kachel(
    titel: String,
    text: String,
    farbe: Color,
    inhaltFarbe: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val inhalt: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(titel, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = farbe,
            contentColor = inhaltFarbe,
            modifier = modifier,
            content = inhalt,
        )
    } else {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = farbe.copy(alpha = 0.55f),
            contentColor = inhaltFarbe.copy(alpha = 0.7f),
            modifier = modifier,
            content = inhalt,
        )
    }
}

@Composable
private fun LeerHinweis(onErstenGartenAnlegen: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.leer_hinweis),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onErstenGartenAnlegen, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.ersten_garten_anlegen))
        }
    }
}

/** Farben für die Rahmen der Grundstücke; ein Grundstück behält seine Farbe, weil sie an seiner Nummer hängt. */
private val GRUNDSTUECK_FARBEN = listOf(
    Color(0xFF3F8A4A), Color(0xFFC46A8F), Color(0xFF5B8FB9),
    Color(0xFFE8A33D), Color(0xFF8E6BBF), Color(0xFFC0684A),
)

private fun grundstueckFarbe(id: Long): Color = GRUNDSTUECK_FARBEN[(id % GRUNDSTUECK_FARBEN.size).toInt()]

private val TAG_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun formatiereTag(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TAG_FORMAT)

/**
 * Ein Grundstück als Block mit farbigem Rahmen: oben Name und Anzahl (Tipp klappt auf und zu, langes Drücken
 * oder die Punkte öffnen das Menü), darunter die Gärten in einer seitlich scrollbaren Reihe.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrundstueckBlock(
    grundstueck: Grundstueck,
    anzahlGaerten: Int,
    offen: Boolean,
    farbe: Color,
    menueOffen: Boolean,
    onKlick: () -> Unit,
    onMenueOeffnen: () -> Unit,
    onMenueSchliessen: () -> Unit,
    onUmbenennen: () -> Unit,
    onLoeschen: () -> Unit,
    inhalt: @Composable () -> Unit,
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        val form = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(form)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(2.dp, farbe, form),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onKlick, onLongClick = onMenueOeffnen)
                    .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(farbe))
                Column(Modifier.weight(1f)) {
                    Text(grundstueck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (anzahlGaerten == 1) {
                            stringResource(R.string.ein_garten)
                        } else {
                            stringResource(R.string.n_gaerten, anzahlGaerten)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painterResource(if (offen) R.drawable.ic_pfeil_hoch else R.drawable.ic_pfeil_runter),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                IconButton(onClick = onMenueOeffnen) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
            }
            if (offen) {
                Box(Modifier.padding(bottom = 12.dp)) { inhalt() }
            }
        }
        DropdownMenu(expanded = menueOffen, onDismissRequest = onMenueSchliessen) {
            DropdownMenuItem(text = { Text(stringResource(R.string.umbenennen)) }, onClick = onUmbenennen)
            DropdownMenuItem(text = { Text(stringResource(R.string.loeschen)) }, onClick = onLoeschen)
        }
    }
}

/**
 * Seitlich scrollbare Reihe von Gartenkacheln; mit [onNeu] steht am Ende eine Kachel zum Hinzufügen.
 * Langes Drücken auf eine Kachel hebt sie an (Vibration), dann lässt sie sich an einen anderen Platz ziehen
 * ([onOrdnen] bekommt danach die neue Reihenfolge). Die Berührung wird an der Reihe selbst ausgewertet, nicht an
 * der Kachel: Die Kachel bewegt sich ja mit dem Finger, und ihre eigenen Koordinaten würden mitwandern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GartenReihe(
    gaerten: List<Garten>,
    seitenRand: androidx.compose.ui.unit.Dp,
    hintergrund: Color,
    karte: @Composable (Garten, Modifier) -> Unit,
    onNeu: (() -> Unit)?,
    onOrdnen: (List<Long>) -> Unit,
) {
    var anzeige by remember(gaerten) { mutableStateOf(gaerten) }
    var gezogenId by remember { mutableStateOf<Long?>(null) }
    var versatz by remember { mutableFloatStateOf(0f) }
    val scrollZustand = androidx.compose.foundation.rememberScrollState()
    var breitePx by remember { mutableFloatStateOf(0f) }
    val dichte = LocalDensity.current
    val randPx = with(dichte) { 72.dp.toPx() }
    val kartePx = with(dichte) { 168.dp.toPx() }
    val schrittPx = with(dichte) { (168.dp + 10.dp).toPx() }
    val randLinksPx = with(dichte) { seitenRand.toPx() }
    val haptik = androidx.compose.ui.platform.LocalHapticFeedback.current
    val gaertenAktuell = rememberUpdatedState(gaerten)
    val ordnenAktuell = rememberUpdatedState(onOrdnen)

    // Wie weit die Reihe gescrollt ist (Pixel vom Anfang). Bewusst eine einfache Reihe statt LazyRow: Eine LazyRow
    // hält beim Umsortieren die erste sichtbare Kachel fest und verschiebt dabei selbst den Bildausschnitt.
    fun scrollX(): Float = scrollZustand.value.toFloat()

    // Mitte der gezogenen Kachel im Sichtfenster; tauscht sie mit dem Nachbarn, sobald die Mitte dort angekommen ist
    fun pruefeTausch(): Float? {
        val id = gezogenId ?: return null
        val liste = anzeige
        val von = liste.indexOfFirst { it.id == id }
        if (von < 0) return null
        val mitte = randLinksPx + von * schrittPx - scrollX() + kartePx / 2f + versatz
        val nach = kotlin.math.floor((mitte + scrollX() - randLinksPx) / schrittPx).toInt().coerceIn(0, liste.size - 1)
        if (nach != von) {
            val neu = liste.toMutableList()
            neu.add(nach, neu.removeAt(von))
            anzeige = neu
            versatz -= (nach - von) * schrittPx
            return mitte - (nach - von) * schrittPx
        }
        return mitte
    }

    // Am Rand der Reihe weiterscrollen, solange gezogen wird (auch wenn der Finger stillhält)
    LaunchedEffect(gezogenId) {
        if (gezogenId == null) return@LaunchedEffect
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            val mitte = pruefeTausch() ?: break
            val schritt = if (mitte < randPx) -14f else if (mitte > breitePx - randPx) 14f else 0f
            if (schritt != 0f) {
                versatz += scrollZustand.scrollBy(schritt)
                pruefeTausch()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().onSizeChanged { breitePx = it.width.toFloat() }.pointerInput(Unit) {
            awaitEachGesture {
                val runter = awaitFirstDown(requireUnconsumed = false)
                val lang = awaitLongPressOrCancellation(runter.id) ?: return@awaitEachGesture
                val liste = anzeige
                val gesamt = lang.position.x + scrollX() - randLinksPx
                val nr = kotlin.math.floor(gesamt / schrittPx).toInt()
                // Nur Treffer auf eine Kachel zählen, nicht auf die Lücke oder die "Neu"-Kachel
                if (nr !in liste.indices || gesamt - nr * schrittPx > kartePx) return@awaitEachGesture
                haptik.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                gezogenId = liste[nr].id
                versatz = 0f
                var letzte = lang.position
                var bewegt = false
                var summe = 0f
                while (true) {
                    // Als Erstes ansehen und verbrauchen, damit weder die Reihe scrollt noch die Kachel anklickt
                    val ereignis = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val c = ereignis.changes.fastFirstOrNull { it.id == runter.id }
                    if (c == null) {
                        gezogenId = null
                        versatz = 0f
                        anzeige = gaertenAktuell.value
                        return@awaitEachGesture
                    }
                    if (!c.pressed) {
                        c.consume()
                        gezogenId = null
                        versatz = 0f
                        if (bewegt) {
                            val ids = anzeige.map { it.id }
                            if (ids != gaertenAktuell.value.map { it.id }) ordnenAktuell.value(ids)
                        }
                        return@awaitEachGesture
                    }
                    val d = c.position - letzte
                    letzte = c.position
                    summe += abs(d.x) + abs(d.y)
                    if (summe > viewConfiguration.touchSlop) bewegt = true
                    if (bewegt) {
                        versatz += d.x
                        pruefeTausch()
                    }
                    c.consume()
                }
            }
        },
    ) {
    Row(
        modifier = Modifier.horizontalScroll(scrollZustand).padding(horizontal = seitenRand),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (g in anzeige) key(g.id) {
            val gezogen = g.id == gezogenId
            val mod = Modifier
                .zIndex(if (gezogen) 1f else 0f)
                .graphicsLayer {
                    if (gezogen) {
                        translationX = versatz
                        scaleX = 1.06f
                        scaleY = 1.06f
                        shadowElevation = 24f
                    }
                }
            karte(g, mod)
        }
        if (onNeu != null) {
            run {
                Surface(
                    onClick = onNeu,
                    shape = RoundedCornerShape(18.dp),
                    color = hintergrund,
                    modifier = Modifier.width(120.dp).height(172.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text(
                            stringResource(R.string.garten_hinzufuegen),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
    }
}

/** Kachel eines Gartens mit Vorschau, Name und Datum der letzten Änderung. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GartenKarte(
    garten: Garten,
    hintergrund: Color,
    modifier: Modifier,
    menueOffen: Boolean,
    onOeffnen: () -> Unit,
    onMenueOeffnen: () -> Unit,
    onMenueSchliessen: () -> Unit,
    onBearbeiten: () -> Unit,
    onDuplizieren: () -> Unit,
    onUmbenennen: () -> Unit,
    onVerschieben: () -> Unit,
    onLoeschen: () -> Unit,
) {
    Box(modifier = modifier.width(168.dp)) {
        val form = RoundedCornerShape(18.dp)
        Surface(
            onClick = onOeffnen,
            shape = form,
            color = hintergrund,
            modifier = Modifier.fillMaxWidth().clip(form),
        ) {
            Column {
                GartenVorschau(garten, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(garten.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.geaendert_kurz, formatiereTag(garten.geaendertAm)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = onMenueOeffnen, modifier = Modifier.size(40.dp)) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        DropdownMenu(expanded = menueOffen, onDismissRequest = onMenueSchliessen) {
            DropdownMenuItem(text = { Text(stringResource(R.string.bearbeiten)) }, onClick = onBearbeiten)
            DropdownMenuItem(text = { Text(stringResource(R.string.duplizieren)) }, onClick = onDuplizieren)
            DropdownMenuItem(text = { Text(stringResource(R.string.umbenennen)) }, onClick = onUmbenennen)
            DropdownMenuItem(text = { Text(stringResource(R.string.zu_grundstueck_verschieben)) }, onClick = onVerschieben)
            DropdownMenuItem(text = { Text(stringResource(R.string.loeschen)) }, onClick = onLoeschen)
        }
    }
}
