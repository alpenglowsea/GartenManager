package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Bestand
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Gegenstand
import io.github.alpenglowsea.gartenmanager.daten.Grunddaten

/** Eine Zeile in der Zusammenstellung: so viele Pflanzen einer Art an einem Ort. */
private data class Posten(
    val pflanzeId: Long,
    val name: String,
    val latein: String,
    val gruppe: String,
    /** Garten, in dem die Pflanzen stehen; leer = keinem Garten zugeordnet. */
    val gartenId: Long?,
    val anzahl: Int,
    /** Wahr: im Gartenplan gezeichnet. Falsch: nur eingetragen (dann ist [eintrag] gesetzt). */
    val imPlan: Boolean,
    val eintrag: Bestand?,
)

/** Alle Posten einer Pflanzenart. */
private data class Art(
    val pflanzeId: Long,
    val name: String,
    val latein: String,
    val gruppe: String,
    val posten: List<Posten>,
) {
    val summe: Int get() = posten.sumOf { it.anzahl }
}

private const val FILTER_OHNE_PLAN = "\u0000ohne-plan"

/**
 * "Meine Pflanzen": alles, was der Nutzer hat. Zum einen die Pflanzen, die in Gärten gezeichnet sind, zum anderen
 * Einträge ohne Zeichnung (Anzahl, Ort, Datum, Notiz). Sortiert nach Pflanze oder nach Garten.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MeinePflanzenScreen(
    gaerten: List<Garten>,
    imPlan: List<Gegenstand>,
    bestand: List<Bestand>,
    onSpeichern: (Bestand) -> Unit,
    onLoeschen: (Long) -> Unit,
    onGartenOeffnen: (Long) -> Unit,
    onZurueck: () -> Unit,
) {
    var nachGarten by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var aufgeklappt by remember { mutableStateOf(setOf<Long>()) }
    var waehlen by remember { mutableStateOf(false) }
    var bearbeite by remember { mutableStateOf<Bestand?>(null) }

    val gartenNamen = remember(gaerten) { gaerten.associate { it.id to it.name } }

    val alle = remember(imPlan, bestand, gaerten) {
        val posten = ArrayList<Posten>()
        imPlan.filter { it.pflanzeId != null }
            .groupBy { (it.pflanzeId ?: 0L) to it.gartenId }
            .forEach { (schluessel, liste) ->
                val e = liste.first()
                posten.add(
                    Posten(
                        pflanzeId = schluessel.first,
                        name = e.pflanzeName.orEmpty(),
                        latein = e.pflanzeLatein.orEmpty(),
                        gruppe = e.pflanzeGruppe.orEmpty(),
                        gartenId = schluessel.second,
                        anzahl = liste.size,
                        imPlan = true,
                        eintrag = null,
                    ),
                )
            }
        for (b in bestand) {
            posten.add(
                Posten(
                    pflanzeId = b.pflanzeId,
                    name = b.pflanzeName,
                    latein = b.pflanzeLatein,
                    gruppe = b.pflanzeGruppe,
                    // Zeigt der Eintrag auf einen Garten, den es nicht mehr gibt, gilt er als "ohne Garten".
                    gartenId = b.gartenId?.takeIf { it in gartenNamen },
                    anzahl = b.anzahl,
                    imPlan = false,
                    eintrag = b,
                ),
            )
        }
        posten
    }

    val arten = remember(alle) {
        alle.groupBy { it.pflanzeId }.map { (id, liste) ->
            val e = liste.first()
            Art(id, e.name, e.latein, e.gruppe, liste)
        }.sortedBy { Grunddaten.normalisiere(it.name) }
    }
    val gruppenImBestand = remember(arten) { arten.map { it.gruppe }.filter { it.isNotEmpty() }.distinct().sorted() }

    fun passt(p: Posten): Boolean = when (filter) {
        null -> true
        FILTER_OHNE_PLAN -> !p.imPlan
        else -> p.gruppe == filter
    }
    val sichtbareArten = arten.filter { art -> art.posten.any { passt(it) } }

    val ohneGarten = stringResource(R.string.mp_ohne_garten)

    if (waehlen) {
        PflanzenWahlDialog(
            onWahl = { t ->
                waehlen = false
                bearbeite = Bestand(
                    pflanzeId = t.id,
                    pflanzeName = t.hauptname,
                    pflanzeLatein = t.lateinisch,
                    pflanzeGruppe = t.hauptgruppe,
                    anzahl = 1,
                    angelegtAm = System.currentTimeMillis(),
                )
            },
            onAbbrechen = { waehlen = false },
        )
    }
    bearbeite?.let { start ->
        BestandSheet(
            start = start,
            gaerten = gaerten,
            onSpeichern = {
                onSpeichern(it)
                bearbeite = null
            },
            onLoeschen = if (start.id != 0L) {
                {
                    onLoeschen(start.id)
                    bearbeite = null
                }
            } else {
                null
            },
            onAbbruch = { bearbeite = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mp_titel)) },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { waehlen = true },
                icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                text = { Text(stringResource(R.string.mp_hinzufuegen)) },
            )
        },
    ) { innen ->
        Column(Modifier.padding(innen).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                SegmentedButton(
                    selected = !nachGarten,
                    onClick = { nachGarten = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.mp_nach_pflanze)) }
                SegmentedButton(
                    selected = nachGarten,
                    onClick = { nachGarten = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.mp_nach_garten)) }
            }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "alle") {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text(stringResource(R.string.mp_filter_alle)) },
                    )
                }
                items(gruppenImBestand, key = { it }) { g ->
                    FilterChip(
                        selected = filter == g,
                        onClick = { filter = if (filter == g) null else g },
                        label = { Text(g) },
                    )
                }
                item(key = "ohne") {
                    FilterChip(
                        selected = filter == FILTER_OHNE_PLAN,
                        onClick = { filter = if (filter == FILTER_OHNE_PLAN) null else FILTER_OHNE_PLAN },
                        label = { Text(stringResource(R.string.mp_filter_ohne_plan)) },
                    )
                }
            }

            if (alle.isEmpty()) {
                Text(
                    stringResource(R.string.mp_leer),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else if (!nachGarten) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                    items(sichtbareArten, key = { it.pflanzeId }) { art ->
                        val offen = art.pflanzeId in aufgeklappt
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            aufgeklappt = if (offen) aufgeklappt - art.pflanzeId else aufgeklappt + art.pflanzeId
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Kreis(art.name, art.gruppe, 42)
                                    Column(Modifier.weight(1f)) {
                                        Text(art.name, style = MaterialTheme.typography.titleMedium)
                                        if (art.latein.isNotEmpty()) {
                                            Text(
                                                art.latein,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Text(
                                        art.summe.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        painterResource(if (offen) R.drawable.ic_pfeil_hoch else R.drawable.ic_pfeil_runter),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                // Chips: wie viele Pflanzen pro Garten (und ohne Garten)
                                val proOrt = art.posten.groupBy { it.gartenId }
                                    .map { (g, liste) -> g to liste.sumOf { it.anzahl } }
                                    .sortedWith(compareBy<Pair<Long?, Int>>({ it.first == null }, { gartenNamen[it.first].orEmpty().lowercase() }))
                                FlowRow(
                                    Modifier.padding(start = 66.dp, end = 12.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    for ((g, n) in proOrt) {
                                        OrtChip(
                                            text = (if (g == null) stringResource(R.string.mp_ohne_plan) else gartenNamen[g].orEmpty()) + " · " + n,
                                            gestrichelt = g == null,
                                        )
                                    }
                                }
                                if (offen) {
                                    HorizontalDivider(Modifier.padding(start = 66.dp, end = 12.dp))
                                    for (p in art.posten.sortedWith(compareBy<Posten>({ it.gartenId == null }, { gartenNamen[it.gartenId].orEmpty().lowercase() }, { !it.imPlan }))) {
                                        PostenZeile(
                                            p = p,
                                            ortName = if (p.gartenId == null) ohneGarten else gartenNamen[p.gartenId].orEmpty(),
                                            onOeffnen = { p.gartenId?.let(onGartenOeffnen) },
                                            onAendern = { bearbeite = p.eintrag },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Nach Garten: pro Garten die Arten, die dort stehen, am Ende alles ohne Garten
                val abschnitte = remember(alle, gaerten) {
                    val liste = ArrayList<Pair<Long?, List<Posten>>>()
                    for (g in gaerten.sortedBy { it.name.lowercase() }) {
                        val p = alle.filter { it.gartenId == g.id }
                        if (p.isNotEmpty()) liste.add(g.id to p)
                    }
                    val o = alle.filter { it.gartenId == null }
                    if (o.isNotEmpty()) liste.add(null to o)
                    liste
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                    for ((gartenId, liste) in abschnitte) {
                        val gefiltert = liste.filter { passt(it) }.sortedBy { Grunddaten.normalisiere(it.name) }
                        if (gefiltert.isEmpty()) continue
                        item(key = "kopf-${gartenId ?: -1}") {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    if (gartenId == null) ohneGarten else gartenNamen[gartenId].orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                val res = LocalContext.current.resources
                                val nArten = gefiltert.map { it.pflanzeId }.distinct().size
                                val nPflanzen = gefiltert.sumOf { it.anzahl }
                                Text(
                                    res.getQuantityString(R.plurals.n_arten, nArten, nArten) + " · " +
                                        res.getQuantityString(R.plurals.n_pflanzen, nPflanzen, nPflanzen),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(gefiltert, key = { "p-${gartenId ?: -1}-${it.pflanzeId}-${it.imPlan}-${it.eintrag?.id ?: 0}" }) { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (p.imPlan) p.gartenId?.let(onGartenOeffnen) else bearbeite = p.eintrag
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Kreis(p.name, p.gruppe, 34)
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                    if (!p.imPlan) {
                                        Text(
                                            stringResource(R.string.mp_nur_eingetragen),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    p.anzahl.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Runder Kreis in der Gruppenfarbe mit den Anfangsbuchstaben (wie im Gartenplan). */
@Composable
private fun Kreis(name: String, gruppe: String, groesse: Int) {
    val farbe = gruppenFarbe(gruppe)
    val schrift = if (farbe.luminance() > 0.45f) Color(0xFF1B1B1B) else Color.White
    Box(
        Modifier.size(groesse.dp).clip(CircleShape).background(farbe),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialen(name),
            color = schrift,
            style = if (groesse >= 40) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OrtChip(text: String, gestrichelt: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (gestrichelt) Color.Transparent else MaterialTheme.colorScheme.surface,
        border = if (gestrichelt) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Ein Ort einer Pflanzenart in der aufgeklappten Zeile. */
@Composable
private fun PostenZeile(p: Posten, ortName: String, onOeffnen: () -> Unit, onAendern: () -> Unit) {
    val res = LocalContext.current.resources
    Row(
        Modifier.fillMaxWidth().padding(start = 66.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(ortName, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (p.imPlan) {
                    res.getQuantityString(R.plurals.mp_n_im_plan, p.anzahl, p.anzahl)
                } else {
                    res.getQuantityString(R.plurals.mp_n_eingetragen, p.anzahl, p.anzahl)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tag = p.eintrag?.pflanzdatum
            if (tag != null) {
                Text(pflanzdatumText(tag), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val notiz = p.eintrag?.notiz
            if (!notiz.isNullOrBlank()) {
                Text(
                    kuerze(notiz, KARTE_NOTIZ_ZEICHEN),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (p.imPlan) {
            if (p.gartenId != null) {
                TextButton(onClick = onOeffnen) { Text(stringResource(R.string.mp_im_plan)) }
            }
        } else {
            TextButton(onClick = onAendern) { Text(stringResource(R.string.mp_aendern)) }
        }
    }
}

/** Fenster von unten: Anzahl, Ort, Datum und Notiz zu einem Eintrag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BestandSheet(
    start: Bestand,
    gaerten: List<Garten>,
    onSpeichern: (Bestand) -> Unit,
    onLoeschen: (() -> Unit)?,
    onAbbruch: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var anzahl by remember { mutableIntStateOf(start.anzahl) }
    var gartenId by remember { mutableStateOf(start.gartenId?.takeIf { id -> gaerten.any { it.id == id } }) }
    var datum by remember { mutableStateOf(start.pflanzdatum) }
    var notiz by remember { mutableStateOf(start.notiz.orEmpty()) }
    var ortMenue by remember { mutableStateOf(false) }
    var datumDialog by remember { mutableStateOf(false) }
    var loeschenFrage by remember { mutableStateOf(false) }

    if (datumDialog) {
        PflanzdatumDialog(
            startTag = datum,
            onSpeichern = {
                datum = it
                datumDialog = false
            },
            onAbbruch = { datumDialog = false },
        )
    }
    if (loeschenFrage && onLoeschen != null) {
        BestaetigenDialog(
            titel = stringResource(R.string.mp_loeschen_titel),
            text = stringResource(R.string.mp_loeschen_text, start.pflanzeName),
            bestaetigenText = stringResource(R.string.loeschen),
            onBestaetigt = {
                loeschenFrage = false
                onLoeschen()
            },
            onAbbruch = { loeschenFrage = false },
        )
    }

    ModalBottomSheet(onDismissRequest = onAbbruch, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kreis(start.pflanzeName, start.pflanzeGruppe, 44)
                Column {
                    Text(start.pflanzeName, style = MaterialTheme.typography.titleMedium)
                    if (start.pflanzeLatein.isNotEmpty()) {
                        Text(
                            start.pflanzeLatein,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(stringResource(R.string.mp_anzahl), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { if (anzahl > 1) anzahl-- }, enabled = anzahl > 1) { Text("−") }
                Text(
                    anzahl.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.width(72.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                OutlinedButton(onClick = { if (anzahl < 999) anzahl++ }, enabled = anzahl < 999) { Text("+") }
            }
            Text(stringResource(R.string.mp_wo), style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(onClick = { ortMenue = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(gaerten.firstOrNull { it.id == gartenId }?.name ?: stringResource(R.string.mp_ohne_garten))
                }
                DropdownMenu(expanded = ortMenue, onDismissRequest = { ortMenue = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mp_ohne_garten)) },
                        onClick = {
                            gartenId = null
                            ortMenue = false
                        },
                    )
                    for (g in gaerten.sortedBy { it.name.lowercase() }) {
                        DropdownMenuItem(
                            text = { Text(g.name) },
                            onClick = {
                                gartenId = g.id
                                ortMenue = false
                            },
                        )
                    }
                }
            }
            Text(stringResource(R.string.mp_datum), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { datumDialog = true }, modifier = Modifier.fillMaxWidth()) {
                val tag = datum
                Text(if (tag == null) stringResource(R.string.mp_datum_waehlen) else pflanzdatumText(tag))
            }
            OutlinedTextField(
                value = notiz,
                onValueChange = { if (it.length <= MAX_PFLANZ_NOTIZ) notiz = it },
                label = { Text(stringResource(R.string.mp_notiz)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onLoeschen != null) {
                    TextButton(onClick = { loeschenFrage = true }) { Text(stringResource(R.string.loeschen)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
                Button(
                    onClick = {
                        onSpeichern(
                            start.copy(
                                anzahl = anzahl,
                                gartenId = gartenId,
                                pflanzdatum = datum,
                                notiz = notiz.trim().ifEmpty { null },
                            ),
                        )
                    },
                ) { Text(stringResource(if (start.id == 0L) R.string.mp_eintragen else R.string.speichern)) }
            }
        }
    }
}
