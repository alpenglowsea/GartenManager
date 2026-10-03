package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten
import io.github.alpenglowsea.gartenmanager.daten.GrunddatenInfo
import io.github.alpenglowsea.gartenmanager.daten.GruppeMitAnzahl
import io.github.alpenglowsea.gartenmanager.daten.PflanzeTreffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vorläufiger Testbildschirm für Teilschritt 4a: zeigt, dass die mitgelieferte Grunddaten-Datei
 * ankommt und gelesen wird. Wird in 4c durch den echten Auswahl-Dialog ersetzt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PflanzenTestScreen(onZurueck: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<GrunddatenInfo?>(null) }
    var gruppen by remember { mutableStateOf<List<GruppeMitAnzahl>>(emptyList()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var suchtext by remember { mutableStateOf("") }
    var gewaehlt by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var treffer by remember { mutableStateOf<List<PflanzeTreffer>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val i = Grunddaten.info(context)
                val g = Grunddaten.gruppen(context)
                info = i
                gruppen = g
            }
        } catch (e: Exception) {
            fehler = e.toString()
        }
    }
    LaunchedEffect(suchtext, gewaehlt, info) {
        if (info == null) return@LaunchedEffect
        try {
            treffer = withContext(Dispatchers.IO) { Grunddaten.suche(context, suchtext, gewaehlt) }
        } catch (e: Exception) {
            fehler = e.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pflanzen_test_titel)) },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
    ) { innen ->
        Column(Modifier.padding(innen).fillMaxSize()) {
            if (fehler != null) {
                Text(
                    stringResource(R.string.pflanzen_test_fehler, fehler ?: ""),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            info?.let {
                Text(
                    stringResource(R.string.pflanzen_test_info, it.version, it.anzahlPflanzen),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (it.testdaten) {
                    Text(
                        stringResource(R.string.pflanzen_test_hinweis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            OutlinedTextField(
                value = suchtext,
                onValueChange = { suchtext = it },
                label = { Text(stringResource(R.string.pflanzen_test_suche)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(gruppen, key = { it.id }) { g ->
                    FilterChip(
                        selected = g.id in gewaehlt,
                        onClick = { gewaehlt = if (g.id in gewaehlt) gewaehlt - g.id else gewaehlt + g.id },
                        label = { Text("${g.name} (${g.anzahl})") },
                    )
                }
            }
            Text(
                stringResource(R.string.pflanzen_test_treffer, treffer.size),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(treffer, key = { it.id }) { t ->
                    ListItem(
                        headlineContent = { Text(t.hauptname) },
                        supportingContent = {
                            Column {
                                Text(t.lateinisch, fontStyle = FontStyle.Italic)
                                if (t.gefundenUeber != null) {
                                    Text(stringResource(R.string.pflanzen_test_gefunden_ueber, t.gefundenUeber))
                                }
                            }
                        },
                        trailingContent = { Text(t.hauptgruppe, style = MaterialTheme.typography.labelMedium) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
