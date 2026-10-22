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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.alpenglowsea.gartenmanager.daten.BildAusschnitt
import io.github.alpenglowsea.gartenmanager.daten.EIGENE_BASIS
import io.github.alpenglowsea.gartenmanager.daten.EigeneAngabe
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import io.github.alpenglowsea.gartenmanager.daten.MerkerZeile
import io.github.alpenglowsea.gartenmanager.daten.PflanzeDetail
import io.github.alpenglowsea.gartenmanager.daten.QUELLE_EIGEN
import io.github.alpenglowsea.gartenmanager.daten.QuelleInfo
import io.github.alpenglowsea.gartenmanager.daten.baueEigenesDetail
import io.github.alpenglowsea.gartenmanager.daten.istEigenePflanze
import io.github.alpenglowsea.gartenmanager.daten.uebernehmeAlteMerker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    else -> {
        // Zum Beispiel "CC BY 2.5" oder "CC BY-SA 3.0 de": Art und Version stehen im Namen
        val m = Regex("""CC BY(-SA)? (\d\.\d)""").find(lizenz)
        if (m == null) null
        else "https://creativecommons.org/licenses/by" + (if (m.groupValues[1].isEmpty()) "" else "-sa") + "/" + m.groupValues[2] + "/"
    }
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
                if (e is kotlinx.coroutines.CancellationException) throw e
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
    val eigene = istEigenePflanze(pflanzeId)
    var detail by remember(pflanzeId) { mutableStateOf<PflanzeDetail?>(null) }
    var fehler by remember(pflanzeId) { mutableStateOf<String?>(null) }
    var voll by rememberSaveable(pflanzeId) { mutableStateOf(false) }
    val dao = remember { MeineDatenDb.holen(context).gartenDao() }
    val ausschnitt by remember(pflanzeId) { dao.ausschnittFuer(pflanzeId) }.collectAsState(initial = null)
    val bereich = rememberCoroutineScope()
    // Zugeklappte Abschnitte (null = noch nicht gelesen), eigene Texte, bei eigenen Pflanzen die Pflanze selbst
    val zugeklappt by remember(pflanzeId) { dao.merkerFuer(pflanzeId) }.collectAsState(initial = null)
    val meineAngaben by remember(pflanzeId) { dao.eigeneAngabenFuer(pflanzeId) }.collectAsState(initial = emptyList())
    val eigenePflanze by remember(pflanzeId) { dao.eigenePflanzeFlow(pflanzeId - EIGENE_BASIS) }.collectAsState(initial = null)
    var notizAbschnitt by remember(pflanzeId) { mutableStateOf<String?>(null) }
    var editorOffen by remember(pflanzeId) { mutableStateOf(false) }
    var loeschenFrage by remember(pflanzeId) { mutableStateOf(false) }

    LaunchedEffect(pflanzeId) {
        try {
            withContext(Dispatchers.IO) { uebernehmeAlteMerker(context, dao) }
            if (eigene) {
                if (dao.eigenePflanze(pflanzeId - EIGENE_BASIS) == null) fehler = context.getString(R.string.info_nicht_gefunden)
            } else {
                val d = withContext(Dispatchers.IO) { Grunddaten.pflanze(context, pflanzeId) }
                if (d == null) fehler = context.getString(R.string.info_nicht_gefunden) else detail = d
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fehler = context.getString(R.string.info_laden_fehler, e.toString())
        }
    }
    val anzeige: PflanzeDetail? = if (eigene) eigenePflanze?.let { baueEigenesDetail(it, meineAngaben) } else detail

    val grossesBild = anzeige?.bild
    if (voll && grossesBild != null) {
        BildVollansicht(
            bild = grossesBild,
            name = anzeige?.hauptname ?: "",
            ausschnitt = ausschnitt,
            onSpeichern = { x, y, z -> bereich.launch { dao.speichereAusschnitt(BildAusschnitt(pflanzeId, x, y, z)) } },
            onZuruecksetzen = { bereich.launch { dao.loescheAusschnitt(pflanzeId) } },
            onZurueck = { voll = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(anzeige?.hauptname ?: "") },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
                actions = {
                    if (eigene && eigenePflanze != null) {
                        IconButton(onClick = { editorOffen = true }) {
                            Icon(painterResource(R.drawable.ic_stift), contentDescription = stringResource(R.string.eigene_pflanze_aendern_titel))
                        }
                        TextButton(onClick = { loeschenFrage = true }) { Text(stringResource(R.string.loeschen)) }
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
            val d = anzeige
            val zuListe = zugeklappt
            if (fehler != null) {
                Text(fehler ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (d != null && zuListe != null) {
                Kopf(d, ausschnitt) { voll = true }
                Spacer(Modifier.height(8.dp))
                for (abschnitt in ABSCHNITTE) {
                    val zu = abschnitt in zuListe
                    AbschnittKopf(abschnitt, zu) {
                        bereich.launch {
                            if (zu) dao.loescheMerker(pflanzeId, abschnitt) else dao.setzeMerker(MerkerZeile(pflanzeId, abschnitt))
                        }
                    }
                    if (!zu) {
                        Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AbschnittInhalt(
                                abschnitt = abschnitt,
                                d = d,
                                eigenePflanze = eigene,
                                notiz = meineAngaben.firstOrNull { it.abschnitt == abschnitt }?.text,
                                onNotiz = { notizAbschnitt = abschnitt },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (eigene) {
                    HorizontalDivider(thickness = 2.dp)
                    Text(
                        stringResource(R.string.info_quellen_titel),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                    Text(stringResource(R.string.eigene_pflanze_quellen), style = MaterialTheme.typography.bodySmall)
                } else {
                    QuellenUndLizenzen(d)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val bearbeiteterAbschnitt = notizAbschnitt
    if (bearbeiteterAbschnitt != null && !eigene) {
        NotizDialog(
            titel = stringResource(R.string.meine_notiz_titel, bearbeiteterAbschnitt),
            startwert = meineAngaben.firstOrNull { it.abschnitt == bearbeiteterAbschnitt }?.text.orEmpty(),
            maxLaenge = MAX_ABSCHNITT_NOTIZ,
            onSpeichern = { text ->
                notizAbschnitt = null
                bereich.launch {
                    if (text.isBlank()) {
                        dao.loescheAngabe(pflanzeId, bearbeiteterAbschnitt)
                    } else {
                        dao.speichereAngabe(EigeneAngabe(pflanzeId, bearbeiteterAbschnitt, text, System.currentTimeMillis()))
                    }
                }
            },
            onAbbruch = { notizAbschnitt = null },
        )
    }
    if (editorOffen && eigene) {
        EigenePflanzeDialog(id = pflanzeId - EIGENE_BASIS, onFertig = { editorOffen = false })
    }
    if (loeschenFrage && eigene) {
        BestaetigenDialog(
            titel = stringResource(R.string.eigene_pflanze_loeschen_titel),
            text = stringResource(R.string.eigene_pflanze_loeschen_text),
            bestaetigenText = stringResource(R.string.loeschen),
            onBestaetigt = {
                loeschenFrage = false
                bereich.launch {
                    dao.loescheEigenePflanze(pflanzeId - EIGENE_BASIS)
                    onZurueck()
                }
            },
            onAbbruch = { loeschenFrage = false },
        )
    }
}

@Composable
private fun Kopf(d: PflanzeDetail, ausschnitt: BildAusschnitt?, onVoll: () -> Unit) {
    val bild = d.bild
    if (bild != null) {
        Column {
            PflanzenKopfbild(bild, d.hauptname, ausschnitt, onVoll)
            Text(
                fotoZeile(bild),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Text(d.hauptname, style = MaterialTheme.typography.headlineSmall)
            Text(d.lateinisch, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            Text(d.hauptgruppe, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PflanzenSymbol(d.hauptgruppe, d.hauptname, 72.dp, mitInitialen = true)
            Column {
                Text(d.hauptname, style = MaterialTheme.typography.headlineSmall)
                Text(d.lateinisch, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                Text(d.hauptgruppe, style = MaterialTheme.typography.labelLarge)
            }
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
private fun AbschnittInhalt(
    abschnitt: String,
    d: PflanzeDetail,
    eigenePflanze: Boolean,
    notiz: String?,
    onNotiz: () -> Unit,
) {
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
            if (d.gattung.isBlank()) {
                Text(stringResource(R.string.info_keine_angaben), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(d.gattung, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            }
        }
        else -> {
            val angaben = d.angaben.filter { it.abschnitt == abschnitt }
            if (angaben.isEmpty()) {
                Text(stringResource(R.string.info_keine_angaben), style = MaterialTheme.typography.bodyMedium)
            } else {
                for (a in angaben) {
                    Text(a.text, style = MaterialTheme.typography.bodyMedium)
                    if (a.quelle == QUELLE_EIGEN) {
                        Text(
                            stringResource(R.string.info_eigene_angabe),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val titel = d.quellen.firstOrNull { it.schluessel == a.quelle }?.titel ?: a.quelle
                        Text(
                            stringResource(if (a.auszug) R.string.info_auszug_quelle else R.string.info_ganz_quelle, titel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!eigenePflanze) {
                // Eigener Text des Nutzers zu diesem Abschnitt (bleibt auf dem Gerät, wird nicht eingereicht)
                if (!notiz.isNullOrBlank()) {
                    Text(
                        stringResource(R.string.meine_notiz_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(notiz, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onNotiz) {
                    Text(stringResource(if (notiz.isNullOrBlank()) R.string.info_selbst_ergaenzen else R.string.meine_notiz_aendern))
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
        val bild = d.bild
        if (bild != null) BildNachweis(bild)
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
