package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Grundstueck

private const val MAX_NAMENSLAENGE = 60

/** Eingabe eines Namens (neuer Garten, Umbenennen, ...). */
@Composable
fun NameDialog(
    titel: String,
    startwert: String,
    bestaetigenText: String,
    onBestaetigt: (String) -> Unit,
    onAbbruch: () -> Unit,
) {
    var text by remember { mutableStateOf(startwert) }
    val gueltig = text.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = onAbbruch,
        title = { Text(titel) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { neu -> if (neu.length <= MAX_NAMENSLAENGE) text = neu },
                singleLine = true,
                label = { Text(stringResource(R.string.name_label)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onBestaetigt(text.trim()) }, enabled = gueltig) {
                Text(bestaetigenText)
            }
        },
        dismissButton = {
            TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

/** Einfache Rückfrage mit Bestätigen und Abbrechen. */
@Composable
fun BestaetigenDialog(
    titel: String,
    text: String,
    bestaetigenText: String,
    onBestaetigt: () -> Unit,
    onAbbruch: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAbbruch,
        title = { Text(titel) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onBestaetigt) { Text(bestaetigenText) }
        },
        dismissButton = {
            TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

/** Auswahl, zu welchem Grundstück ein Garten gehören soll (oder zu keinem). */
@Composable
fun VerschiebenDialog(
    aktuellesGrundstueckId: Long?,
    grundstuecke: List<Grundstueck>,
    onWahl: (Long?) -> Unit,
    onAbbruch: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAbbruch,
        title = { Text(stringResource(R.string.verschieben_titel)) },
        text = {
            Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
                val keinePrefix = if (aktuellesGrundstueckId == null) "✓ " else ""
                TextButton(onClick = { onWahl(null) }) {
                    Text(keinePrefix + stringResource(R.string.kein_grundstueck))
                }
                for (g in grundstuecke) {
                    val prefix = if (g.id == aktuellesGrundstueckId) "✓ " else ""
                    TextButton(onClick = { onWahl(g.id) }) { Text(prefix + g.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

/** Grundstück mit Gärten löschen: Gruppe auflösen oder alles löschen. */
@Composable
fun GrundstueckLoeschenDialog(
    name: String,
    onAufloesen: () -> Unit,
    onMitGaertenLoeschen: () -> Unit,
    onAbbruch: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAbbruch,
        title = { Text(stringResource(R.string.grundstueck_loeschen_titel, name)) },
        text = { Text(stringResource(R.string.grundstueck_loeschen_text)) },
        confirmButton = {
            Row {
                TextButton(onClick = onAufloesen) {
                    Text(stringResource(R.string.grundstueck_aufloesen))
                }
                TextButton(onClick = onMitGaertenLoeschen) {
                    Text(stringResource(R.string.grundstueck_mit_gaerten_loeschen))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}
