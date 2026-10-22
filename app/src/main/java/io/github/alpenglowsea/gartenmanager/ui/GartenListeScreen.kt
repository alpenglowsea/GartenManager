package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
    onKatalog: () -> Unit,
    onMeinePflanzen: () -> Unit,
    onUeber: () -> Unit,
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
                            GrundstueckZeile(
                                grundstueck = grundstueck,
                                anzahlGaerten = seineGaerten.size,
                                offen = offen,
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
                            )
                        }
                        if (offen) {
                            for (garten in seineGaerten) {
                                item(key = "garten-${garten.id}") {
                                    GartenZeile(
                                        garten = garten,
                                        eingerueckt = true,
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
                            }
                            item(key = "hinzufuegen-${grundstueck.id}") {
                                TextButton(
                                    onClick = { dialog = DialogZustand.NeuerGarten(grundstueck.id) },
                                    modifier = Modifier.padding(start = 32.dp),
                                ) { Text(stringResource(R.string.garten_hinzufuegen)) }
                            }
                        }
                    }
                    if (grundstuecke.isNotEmpty() && einzelne.isNotEmpty()) {
                        item(key = "trenner") { Abschnittsname(stringResource(R.string.einzelne_gaerten)) }
                    }
                    for (garten in einzelne) {
                        item(key = "garten-${garten.id}") {
                            GartenZeile(
                                garten = garten,
                                eingerueckt = false,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrundstueckZeile(
    grundstueck: Grundstueck,
    anzahlGaerten: Int,
    offen: Boolean,
    menueOffen: Boolean,
    onKlick: () -> Unit,
    onMenueOeffnen: () -> Unit,
    onMenueSchliessen: () -> Unit,
    onUmbenennen: () -> Unit,
    onLoeschen: () -> Unit,
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        val form = RoundedCornerShape(20.dp)
        Surface(
            shape = form,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth().clip(form).combinedClickable(onClick = onKlick, onLongClick = onMenueOeffnen),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painterResource(if (offen) R.drawable.ic_pfeil_hoch else R.drawable.ic_pfeil_runter),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(grundstueck.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (anzahlGaerten == 1) {
                            stringResource(R.string.ein_garten)
                        } else {
                            stringResource(R.string.n_gaerten, anzahlGaerten)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onMenueOeffnen) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
            }
        }
        DropdownMenu(expanded = menueOffen, onDismissRequest = onMenueSchliessen) {
            DropdownMenuItem(text = { Text(stringResource(R.string.umbenennen)) }, onClick = onUmbenennen)
            DropdownMenuItem(text = { Text(stringResource(R.string.loeschen)) }, onClick = onLoeschen)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GartenZeile(
    garten: Garten,
    eingerueckt: Boolean,
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
    Box(modifier = Modifier.padding(start = if (eingerueckt) 32.dp else 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)) {
        val form = RoundedCornerShape(20.dp)
        Surface(
            shape = form,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().clip(form).combinedClickable(onClick = onOeffnen, onLongClick = onMenueOeffnen),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Vorläufig der Anfangsbuchstabe; eine kleine Vorschau des Plans kommt später.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = garten.name.trim().take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(garten.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.zuletzt_geaendert, formatiere(garten.geaendertAm)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMenueOeffnen) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
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
