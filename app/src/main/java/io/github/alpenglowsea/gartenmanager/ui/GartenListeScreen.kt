package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
    onPflanzenTest: () -> Unit,
) {
    var dialog by remember { mutableStateOf<DialogZustand>(DialogZustand.Keiner) }
    var menue by remember { mutableStateOf<MenueZiel?>(null) }
    var fabMenue by remember { mutableStateOf(false) }
    val kopieZusatz = stringResource(R.string.kopie_zusatz)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onPflanzenTest) { Text(stringResource(R.string.pflanzen_test_menue)) }
                },
            )
        },
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
            } else if (grundstuecke.isEmpty() && gaerten.isEmpty()) {
                LeerHinweis(onErstenGartenAnlegen = { dialog = DialogZustand.NeuerGarten(null) })
            } else {
                val einzelne = gaerten.filter { it.grundstueckId == null }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                    modifier = Modifier.padding(start = 24.dp),
                                ) { Text(stringResource(R.string.garten_hinzufuegen)) }
                            }
                        }
                    }
                    if (grundstuecke.isNotEmpty() && einzelne.isNotEmpty()) {
                        item(key = "trenner") {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = stringResource(R.string.einzelne_gaerten),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }
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
                    // Platz unter der letzten Zeile, damit der Plus-Knopf nichts verdeckt.
                    item(key = "platzhalter-unten") { Box(modifier = Modifier.padding(bottom = 88.dp)) {} }
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
private fun LeerHinweis(onErstenGartenAnlegen: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
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
    Box {
        ListItem(
            leadingContent = { Text(if (offen) "▼" else "▶") },
            headlineContent = { Text(grundstueck.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Text(
                    if (anzahlGaerten == 1) {
                        stringResource(R.string.ein_garten)
                    } else {
                        stringResource(R.string.n_gaerten, anzahlGaerten)
                    },
                )
            },
            trailingContent = {
                IconButton(onClick = onMenueOeffnen) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onKlick, onLongClick = onMenueOeffnen),
        )
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
    Box(modifier = Modifier.padding(start = if (eingerueckt) 24.dp else 0.dp)) {
        ListItem(
            headlineContent = { Text(garten.name) },
            supportingContent = {
                Text(stringResource(R.string.zuletzt_geaendert, formatiere(garten.geaendertAm)))
            },
            trailingContent = {
                IconButton(onClick = onMenueOeffnen) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOeffnen, onLongClick = onMenueOeffnen),
        )
        DropdownMenu(expanded = menueOffen, onDismissRequest = onMenueSchliessen) {
            DropdownMenuItem(text = { Text(stringResource(R.string.bearbeiten)) }, onClick = onBearbeiten)
            DropdownMenuItem(text = { Text(stringResource(R.string.duplizieren)) }, onClick = onDuplizieren)
            DropdownMenuItem(text = { Text(stringResource(R.string.umbenennen)) }, onClick = onUmbenennen)
            DropdownMenuItem(text = { Text(stringResource(R.string.zu_grundstueck_verschieben)) }, onClick = onVerschieben)
            DropdownMenuItem(text = { Text(stringResource(R.string.loeschen)) }, onClick = onLoeschen)
        }
    }
}
