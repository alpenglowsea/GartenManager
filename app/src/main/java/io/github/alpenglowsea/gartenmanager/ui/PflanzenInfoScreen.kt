package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten
import io.github.alpenglowsea.gartenmanager.daten.Merker
import io.github.alpenglowsea.gartenmanager.daten.PflanzeDetail
import io.github.alpenglowsea.gartenmanager.daten.QuelleInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Die acht Abschnitte des Info-Fensters. Die Namen der Textabschnitte entsprechen denen in den Grunddaten. */
private val ABSCHNITTE = listOf(
    "Bezeichnung", "Gattung", "Herkunft", "Standort", "Wuchs und Laub", "Blüte", "Verwendung", "Pflege",
)

/** Adresse des Lizenztextes, soweit wir die Lizenz kennen. */
internal fun lizenzAdresse(lizenz: String?): String? = when {
    lizenz == null -> null
    lizenz.startsWith("CC BY-SA 4.0") -> "https://creativecommons.org/licenses/by-sa/4.0/deed.de"
    lizenz.startsWith("CC0") -> "https://creativecommons.org/publicdomain/zero/1.0/deed.de"
    else -> null
}

/** Eine antippbare Internetadresse. Das öffnet den Browser des Geräts; die App selbst braucht kein Internet. */
@Composable
internal fun LinkText(adresse: String, modifier: Modifier = Modifier, anzeige: String = adresse) {
    val uri = LocalUriHandler.current
    Text(
        text = anzeige,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.clickable {
            try {
                uri.openUri(adresse)
            } catch (e: Exception) {
                // Kein Browser vorhanden: nichts tun
            }
        },
    )
}

/**
 * Info-Fenster zu einer Pflanze: acht Abschnitte (aufklappbar, Zustand wird je Pflanzenart gemerkt)
 * und unten nicht wegklappbar "Quellen und Lizenzen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PflanzenInfoScreen(pflanzeId: Long, onZurueck: () -> Unit) {
    val context = LocalContext.current
    val merker = remember { Merker(context) }
    var detail by remember(pflanzeId) { mutableStateOf<PflanzeDetail?>(null) }
    var fehler by remember(pflanzeId) { mutableStateOf<String?>(null) }
    var zugeklappt by remember(pflanzeId) { mutableStateOf(merker.zugeklappt(pflanzeId)) }

    LaunchedEffect(pflanzeId) {
        try {
            val d = withContext(Dispatchers.IO) { Grunddaten.pflanze(context, pflanzeId) }
            if (d == null) fehler = context.getString(R.string.info_nicht_gefunden) else detail = d
        } catch (e: Exception) {
            fehler = context.getString(R.string.info_laden_fehler, e.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.hauptname ?: "") },
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
        ) {
            val d = detail
            if (fehler != null) {
                Text(fehler ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (d != null) {
                Kopf(d)
                Spacer(Modifier.height(8.dp))
                for (abschnitt in ABSCHNITTE) {
                    val zu = abschnitt in zugeklappt
                    AbschnittKopf(abschnitt, zu) {
                        merker.setze(pflanzeId, abschnitt, !zu)
                        zugeklappt = merker.zugeklappt(pflanzeId)
                    }
                    if (!zu) {
                        Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AbschnittInhalt(abschnitt, d)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                QuellenUndLizenzen(d)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Kopf(d: PflanzeDetail) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PflanzenSymbol(d.hauptgruppe, d.hauptname, 72.dp, mitInitialen = true)
        Column {
            Text(d.hauptname, style = MaterialTheme.typography.headlineSmall)
            Text(d.lateinisch, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            Text(d.hauptgruppe, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AbschnittKopf(titel: String, zugeklappt: Boolean, onKlick: () -> Unit) {
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onKlick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titel, style = MaterialTheme.typography.titleMedium)
        Text(if (zugeklappt) "▸" else "▾", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AbschnittInhalt(abschnitt: String, d: PflanzeDetail) {
    when (abschnitt) {
        "Bezeichnung" -> {
            Text(d.hauptname, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(d.lateinisch, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            val umgang = d.namen.filter { it.art == "umgangssprachlich" }.map { it.name }
            if (umgang.isNotEmpty()) Text(stringResource(R.string.info_auch_bekannt, umgang.joinToString(", ")))
            val syn = d.namen.filter { it.art == "lateinisch" }.map { it.name }
            if (syn.isNotEmpty()) Text(stringResource(R.string.info_synonyme, syn.joinToString(", ")), fontStyle = FontStyle.Italic)
            Text(stringResource(R.string.info_gruppen, d.gruppen.joinToString(", ")))
        }
        "Gattung" -> {
            Text(d.gattung, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
        }
        else -> {
            val angaben = d.angaben.filter { it.abschnitt == abschnitt }
            if (angaben.isEmpty()) {
                Text(stringResource(R.string.info_keine_angaben), style = MaterialTheme.typography.bodyMedium)
                // Wird in Teilschritt 4e freigeschaltet
                TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.info_selbst_ergaenzen)) }
            } else {
                for (a in angaben) {
                    Text(a.text, style = MaterialTheme.typography.bodyMedium)
                    val titel = d.quellen.firstOrNull { it.schluessel == a.quelle }?.titel ?: a.quelle
                    Text(
                        stringResource(if (a.auszug) R.string.info_auszug_quelle else R.string.info_ganz_quelle, titel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuellenUndLizenzen(d: PflanzeDetail) {
    HorizontalDivider(thickness = 2.dp)
    Text(
        stringResource(R.string.info_quellen_titel),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 14.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (d.wikipedia != null || d.wikidata != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (d.wikipedia != null) Text(stringResource(R.string.info_wikipedia_artikel, d.wikipedia), style = MaterialTheme.typography.bodySmall)
                if (d.wikidata != null) Text(stringResource(R.string.info_wikidata, d.wikidata), style = MaterialTheme.typography.bodySmall)
            }
        }
        for (q in d.quellen) QuelleBlock(q)
        Text(stringResource(R.string.info_zusammenstellung), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QuelleBlock(q: QuelleInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(q.titel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (q.lizenz != null) {
            Text(stringResource(R.string.info_lizenz, q.lizenz), style = MaterialTheme.typography.bodySmall)
            val la = lizenzAdresse(q.lizenz)
            if (la != null) LinkText(la)
        }
        if (q.adresse != null) LinkText(q.adresse)
        if (q.version != null) Text(stringResource(R.string.info_version, q.version), style = MaterialTheme.typography.bodySmall)
        if (q.abrufdatum != null) Text(stringResource(R.string.info_abruf, q.abrufdatum), style = MaterialTheme.typography.bodySmall)
        if (q.aenderung != null) Text(stringResource(R.string.info_aenderung, q.aenderung), style = MaterialTheme.typography.bodySmall)
    }
}
