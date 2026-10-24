package io.github.alpenglowsea.gartenmanager.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Datensicherung
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Das Wort, das man zur Bestätigung tippen muss, bevor alles gelöscht wird. */
private const val BESTAETIGUNGSWORT = "LÖSCHEN"

/** "Optionen": Sicherung erstellen, Sicherung einspielen, alle Daten löschen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionenScreen(onZurueck: () -> Unit) {
    val context = LocalContext.current
    val bereich = rememberCoroutineScope()
    var arbeitet by remember { mutableStateOf<String?>(null) } // Text, solange etwas läuft
    var meldung by remember { mutableStateOf<Pair<String, String>?>(null) } // Titel und Text
    var einspielenFrage by remember { mutableStateOf<Datensicherung.Pruefung?>(null) }
    var loeschenFrage by remember { mutableStateOf(false) }

    val speichern = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            arbeitet = context.getString(R.string.opt_arbeitet_sichern)
            bereich.launch {
                val ergebnis = withContext(Dispatchers.IO) { Datensicherung.exportiere(context, uri) }
                arbeitet = null
                meldung = if (ergebnis != null) {
                    context.getString(R.string.opt_sicherung_fertig_titel) to
                        context.getString(R.string.opt_sicherung_fertig_text, ergebnis.first, ergebnis.second)
                } else {
                    context.getString(R.string.opt_fehler_titel) to context.getString(R.string.opt_sicherung_fehler)
                }
            }
        }
    }
    val oeffnen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            arbeitet = context.getString(R.string.opt_arbeitet_pruefen)
            bereich.launch {
                val pruefung = withContext(Dispatchers.IO) { Datensicherung.pruefe(context, uri) }
                arbeitet = null
                if (pruefung.fehler == null) {
                    einspielenFrage = pruefung
                } else {
                    val text = when (pruefung.fehler) {
                        Datensicherung.Fehler.KEINE_SICHERUNG -> R.string.opt_fehler_keine_sicherung
                        Datensicherung.Fehler.ZU_NEU -> R.string.opt_fehler_zu_neu
                        Datensicherung.Fehler.ZU_ALT -> R.string.opt_fehler_zu_alt
                        Datensicherung.Fehler.BESCHAEDIGT -> R.string.opt_fehler_beschaedigt
                        else -> R.string.opt_fehler_lesen
                    }
                    meldung = context.getString(R.string.opt_einspielen_fehler_titel) to context.getString(text)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.opt_titel)) },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
    ) { innen ->
        Column(
            Modifier
                .padding(innen)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Block(titel = stringResource(R.string.opt_sicherung_titel)) {
                Text(stringResource(R.string.opt_sicherung_text), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { speichern.launch(Datensicherung.vorschlagName()) },
                    enabled = arbeitet == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.opt_sicherung_erstellen)) }
                OutlinedButton(
                    onClick = { oeffnen.launch(arrayOf("*/*")) },
                    enabled = arbeitet == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.opt_sicherung_einspielen)) }
            }
            Block(titel = stringResource(R.string.opt_loeschen_titel)) {
                Text(stringResource(R.string.opt_loeschen_text), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { loeschenFrage = true },
                    enabled = arbeitet == null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.opt_loeschen_knopf)) }
            }
        }
    }

    val laeuft = arbeitet
    if (laeuft != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text(laeuft)
                }
            },
        )
    }
    val m = meldung
    if (m != null) {
        AlertDialog(
            onDismissRequest = { meldung = null },
            title = { Text(m.first) },
            text = { Text(m.second) },
            confirmButton = { TextButton(onClick = { meldung = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
    val frage = einspielenFrage
    if (frage != null) {
        val wann = if (frage.erstellt.isBlank()) stringResource(R.string.opt_unbekannt) else frage.erstellt
        AlertDialog(
            onDismissRequest = {
                einspielenFrage = null
                Datensicherung.raeumeAuf(context)
            },
            title = { Text(stringResource(R.string.opt_einspielen_titel)) },
            text = { Text(stringResource(R.string.opt_einspielen_text, wann, frage.gaerten, frage.fotos)) },
            confirmButton = {
                TextButton(onClick = {
                    einspielenFrage = null
                    arbeitet = context.getString(R.string.opt_arbeitet_einspielen)
                    bereich.launch {
                        val ok = withContext(Dispatchers.IO) { Datensicherung.spieleEin(context, frage) }
                        if (ok) {
                            Datensicherung.starteNeu(context)
                        } else {
                            arbeitet = null
                            meldung = context.getString(R.string.opt_fehler_titel) to context.getString(R.string.opt_einspielen_fehler)
                        }
                    }
                }) { Text(stringResource(R.string.opt_einspielen_bestaetigen), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    einspielenFrage = null
                    Datensicherung.raeumeAuf(context)
                }) { Text(stringResource(R.string.abbrechen)) }
            },
        )
    }
    if (loeschenFrage) {
        var eingabe by remember { mutableStateOf("") }
        val passt = eingabe.trim().equals(BESTAETIGUNGSWORT, ignoreCase = true)
        AlertDialog(
            onDismissRequest = { loeschenFrage = false },
            title = { Text(stringResource(R.string.opt_loeschen_frage_titel)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.opt_loeschen_frage_text))
                    OutlinedTextField(
                        value = eingabe,
                        onValueChange = { eingabe = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.opt_loeschen_eingabe, BESTAETIGUNGSWORT)) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = passt,
                    onClick = {
                        loeschenFrage = false
                        arbeitet = context.getString(R.string.opt_arbeitet_loeschen)
                        bereich.launch {
                            val ok = withContext(Dispatchers.IO) { Datensicherung.loescheAlles(context) }
                            if (ok) {
                                Datensicherung.starteNeu(context)
                            } else {
                                arbeitet = null
                                meldung = context.getString(R.string.opt_fehler_titel) to context.getString(R.string.opt_loeschen_fehler)
                            }
                        }
                    },
                ) { Text(stringResource(R.string.opt_loeschen_bestaetigen), color = if (passt) MaterialTheme.colorScheme.error else Color.Unspecified) }
            },
            dismissButton = { TextButton(onClick = { loeschenFrage = false }) { Text(stringResource(R.string.abbrechen)) } },
        )
    }
}

@Composable
private fun Block(titel: String, inhalt: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            inhalt()
        }
    }
}
