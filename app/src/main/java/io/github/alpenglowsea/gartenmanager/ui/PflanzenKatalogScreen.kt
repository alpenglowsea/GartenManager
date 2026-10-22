package io.github.alpenglowsea.gartenmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import io.github.alpenglowsea.gartenmanager.daten.PflanzeTreffer
import io.github.alpenglowsea.gartenmanager.daten.sucheMitEigenen
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues as KopfPadding
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pflanzenkatalog: durchsuchbare Liste aller Pflanzen der Grunddaten mit Gruppen-Filter.
 * Antippen einer Pflanze öffnet das Info-Fenster.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PflanzenKatalogScreen(onZurueck: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<GrunddatenInfo?>(null) }
    var gruppen by remember { mutableStateOf<List<GruppeMitAnzahl>>(emptyList()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var suchtext by rememberSaveable { mutableStateOf("") }
    var gewaehlt by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var treffer by remember { mutableStateOf<List<PflanzeTreffer>>(emptyList()) }
    var offen by rememberSaveable { mutableStateOf<Long?>(null) }
    val dao = remember { MeineDatenDb.holen(context).gartenDao() }
    val ausschnitte by remember { dao.alleAusschnitte() }.collectAsState(initial = emptyList())
    val ausschnittNachId = remember(ausschnitte) { ausschnitte.associateBy { it.pflanzeId } }
    val eigenePflanzen by remember { dao.eigenePflanzen() }.collectAsState(initial = emptyList())
    var editorOffen by rememberSaveable { mutableStateOf(false) }
    var nurEigene by rememberSaveable { mutableStateOf(false) }
    // Kopf (Suche, Filter, Knopf) wandert beim Scrollen mit und blendet dabei aus; Hochscrollen holt ihn zurück.
    var kopfHoehe by remember { mutableStateOf(0) }
    var kopfVersatz by remember { mutableFloatStateOf(0f) }
    val kopfVerbindung = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                kopfVersatz = (kopfVersatz + available.y).coerceIn(-kopfHoehe.toFloat(), 0f)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val i = Grunddaten.info(context)
                val g = Grunddaten.gruppen(context)
                info = i
                gruppen = g
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fehler = e.toString()
        }
    }
    LaunchedEffect(suchtext, gewaehlt, info, gruppen, eigenePflanzen, nurEigene) {
        if (info == null) return@LaunchedEffect
        try {
            treffer = withContext(Dispatchers.IO) { sucheMitEigenen(context, suchtext, gewaehlt, gruppen, nurEigene) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fehler = e.toString()
        }
    }

    val offeneId = offen
    if (offeneId != null) {
        BackHandler { offen = null }
        PflanzenInfoScreen(pflanzeId = offeneId, onZurueck = { offen = null })
        return
    }

    if (editorOffen) {
        EigenePflanzeDialog(id = 0L, onFertig = { editorOffen = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pflanzenkatalog_titel)) },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
    ) { innen ->
        Box(Modifier.padding(innen).fillMaxSize().nestedScroll(kopfVerbindung)) {
            val kopfDp = with(LocalDensity.current) { kopfHoehe.toDp() }
            // Die Liste liegt unter dem Kopf; der Kopf liegt darüber und wandert/blendet beim Scrollen aus.
            LazyColumn(Modifier.fillMaxSize(), contentPadding = KopfPadding(top = kopfDp)) {
                items(treffer, key = { it.id }) { t ->
                    // Kachel: heller Hintergrund in der Gruppenfarbe, kräftiger Streifen links (Stil B)
                    val farbe = gruppenFarbe(t.hauptgruppe)
                    val hintergrund = lerp(MaterialTheme.colorScheme.surface, farbe, 0.16f)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .height(IntrinsicSize.Min),
                    ) {
                        Box(Modifier.width(8.dp).fillMaxHeight().background(farbe))
                        Column(
                            Modifier
                                .weight(1f)
                                .background(hintergrund)
                                .clickable { offen = t.id }
                                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                        ) {
                            // Schmale Zeile für die Gruppe, damit der Name die ganze übrige Breite bekommt
                            Text(
                                t.hauptgruppe,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                PflanzenKachelBild(
                                    bild = t.bild,
                                    gruppe = t.hauptgruppe,
                                    name = t.hauptname,
                                    ausschnitt = ausschnittNachId[t.id],
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(t.hauptname, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        t.lateinisch,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                    )
                                    if (t.eigene) {
                                        Text(
                                            stringResource(R.string.eigene_pflanze_marke),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else if (t.gefundenUeber != null) {
                                        Text(
                                            stringResource(R.string.pflanzen_test_gefunden_ueber, t.gefundenUeber),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .onSizeChanged { kopfHoehe = it.height }
                    .graphicsLayer {
                        translationY = kopfVersatz
                        alpha = if (kopfHoehe > 0) (1f + kopfVersatz / kopfHoehe).coerceIn(0f, 1f) else 1f
                    }
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                if (fehler != null) {
                    Text(
                        stringResource(R.string.pflanzen_test_fehler, fehler ?: ""),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                info?.let {
                    if (it.testdaten) {
                        Text(
                            stringResource(R.string.pflanzen_test_hinweis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = suchtext,
                    onValueChange = { suchtext = it },
                    label = { Text(stringResource(R.string.pflanzen_test_suche)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "eigene") {
                        FilterChip(
                            selected = nurEigene,
                            onClick = { nurEigene = !nurEigene },
                            label = { Text(stringResource(R.string.eigene_pflanzen_filter)) },
                        )
                    }
                    items(gruppen, key = { it.id }) { g ->
                        FilterChip(
                            selected = g.id in gewaehlt,
                            onClick = { gewaehlt = if (g.id in gewaehlt) gewaehlt - g.id else gewaehlt + g.id },
                            label = { Text("${g.name} (${g.anzahl})") },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.pflanzen_test_treffer, treffer.size),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    TextButton(onClick = { editorOffen = true }) { Text(stringResource(R.string.eigene_pflanze_anlegen)) }
                }
                HorizontalDivider()
            }
        }
    }
}
