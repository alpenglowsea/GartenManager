package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.EIGENE_BASIS
import io.github.alpenglowsea.gartenmanager.daten.EigenePflanze
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import io.github.alpenglowsea.gartenmanager.daten.TEXT_ABSCHNITTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_NAME = 60
private const val MAX_LATEIN = 80
private const val MAX_ABSCHNITT = 2000

/**
 * Anlegen oder Ändern einer eigenen Pflanze als Vollbild-Fenster. [id] = 0: neue Pflanze, sonst die Nummer in der
 * Tabelle eigene_pflanze. Pflicht sind nur Name und Gruppe. [onFertig] bekommt die Nummer der gespeicherten Pflanze
 * (Tabellennummer ohne EIGENE_BASIS) oder null, wenn abgebrochen wurde.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EigenePflanzeDialog(id: Long, onFertig: (Long?) -> Unit) {
    val context = LocalContext.current
    val dao = remember { MeineDatenDb.holen(context).gartenDao() }
    val bereich = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var latein by remember { mutableStateOf("") }
    var gattung by remember { mutableStateOf("") }
    var gruppe by remember { mutableStateOf("") }
    var angelegtAm by remember { mutableStateOf(0L) }
    val texte = remember { mutableStateMapOf<String, String>() }
    var gruppenNamen by remember { mutableStateOf<List<String>>(emptyList()) }
    var menueOffen by remember { mutableStateOf(false) }
    var speichert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gruppenNamen = try {
            withContext(Dispatchers.IO) { Grunddaten.gruppen(context).map { it.name } }
        } catch (e: Exception) {
            emptyList()
        }
    }
    LaunchedEffect(id) {
        if (id != 0L) {
            val p = dao.eigenePflanze(id)
            if (p != null) {
                name = p.name
                latein = p.lateinisch
                gattung = p.gattung
                gruppe = p.gruppe
                angelegtAm = p.angelegtAm
                for (a in dao.eigeneAngabenListe(EIGENE_BASIS + id)) texte[a.abschnitt] = a.text
            }
        }
    }

    val gueltig = name.isNotBlank() && gruppe.isNotBlank() && !speichert

    Dialog(
        onDismissRequest = { onFertig(null) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(stringResource(if (id == 0L) R.string.eigene_pflanze_neu_titel else R.string.eigene_pflanze_aendern_titel))
                        },
                        navigationIcon = {
                            IconButton(onClick = { onFertig(null) }) {
                                Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                            }
                        },
                        actions = {
                            TextButton(
                                enabled = gueltig,
                                onClick = {
                                    speichert = true
                                    bereich.launch {
                                        val neu = dao.speichereEigenePflanze(
                                            EigenePflanze(
                                                id = id,
                                                name = name.trim(),
                                                lateinisch = latein.trim(),
                                                gattung = gattung.trim(),
                                                gruppe = gruppe,
                                                angelegtAm = angelegtAm,
                                            ),
                                            texte.toMap(),
                                            System.currentTimeMillis(),
                                        )
                                        onFertig(neu)
                                    }
                                },
                            ) { Text(stringResource(R.string.speichern)) }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.eigene_pflanze_hinweis), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= MAX_NAME) name = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.eigene_pflanze_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column {
                        Text(stringResource(R.string.eigene_pflanze_gruppe), style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(onClick = { menueOffen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(gruppe.ifEmpty { stringResource(R.string.eigene_pflanze_gruppe_waehlen) })
                        }
                        DropdownMenu(expanded = menueOffen, onDismissRequest = { menueOffen = false }) {
                            for (g in gruppenNamen) {
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = {
                                        gruppe = g
                                        menueOffen = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = latein,
                        onValueChange = { if (it.length <= MAX_LATEIN) latein = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.eigene_pflanze_latein)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = gattung,
                        onValueChange = { if (it.length <= MAX_LATEIN) gattung = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.eigene_pflanze_gattung)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    for (abschnitt in TEXT_ABSCHNITTE) {
                        OutlinedTextField(
                            value = texte[abschnitt].orEmpty(),
                            onValueChange = { if (it.length <= MAX_ABSCHNITT) texte[abschnitt] = it },
                            minLines = 2,
                            maxLines = 8,
                            label = { Text(abschnitt) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
