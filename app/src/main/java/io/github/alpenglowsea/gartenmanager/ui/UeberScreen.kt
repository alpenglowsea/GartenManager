package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten
import io.github.alpenglowsea.gartenmanager.daten.GrunddatenInfo
import io.github.alpenglowsea.gartenmanager.daten.QuellenZaehlung
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PROJEKT_ADRESSE = "https://github.com/alpenglowsea/GartenManager"
private const val GPL_ADRESSE = "https://www.gnu.org/licenses/gpl-3.0.de.html"

/** "Über GartenManager": Version, Lizenzen von Programm und Pflanzendaten, Hinweise. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UeberScreen(onZurueck: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<GrunddatenInfo?>(null) }
    var quellen by remember { mutableStateOf<List<QuellenZaehlung>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val i = Grunddaten.info(context)
                val q = Grunddaten.quellenUebersicht(context)
                info = i
                quellen = q
            }
        } catch (e: Exception) {
            // Ohne Grunddaten bleibt der Datenteil einfach leer.
        }
    }
    val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ueber_titel)) },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
    ) { innen ->
        Column(
            Modifier.padding(innen).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.ueber_version, appVersion), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.ueber_beschreibung), style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()
            Text(stringResource(R.string.ueber_programm_titel), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ueber_programm_text), style = MaterialTheme.typography.bodyMedium)
            LinkText(GPL_ADRESSE)
            LinkText(PROJEKT_ADRESSE)

            HorizontalDivider()
            Text(stringResource(R.string.ueber_daten_titel), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ueber_daten_text), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.ueber_lizenz_wikipedia), style = MaterialTheme.typography.bodyMedium)
            LinkText(lizenzAdresse("CC BY-SA 4.0") ?: "")
            Text(stringResource(R.string.ueber_lizenz_wikidata), style = MaterialTheme.typography.bodyMedium)
            LinkText(lizenzAdresse("CC0") ?: "")
            Text(stringResource(R.string.ueber_lizenz_zusammenstellung), style = MaterialTheme.typography.bodyMedium)
            info?.let {
                Text(
                    stringResource(R.string.pflanzen_test_info, it.version, it.anzahlPflanzen),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (it.testdaten) {
                    Text(
                        stringResource(R.string.pflanzen_test_hinweis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (quellen.isNotEmpty()) {
                Text(stringResource(R.string.ueber_quellen_uebersicht), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                for (q in quellen) {
                    Text(
                        stringResource(R.string.ueber_quellen_zeile, q.anzahl, q.art, q.lizenz ?: "–"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.ueber_hinweis_titel), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ueber_hinweis_text), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
