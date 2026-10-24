package io.github.alpenglowsea.gartenmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alpenglowsea.gartenmanager.ui.GartenListeScreen
import io.github.alpenglowsea.gartenmanager.ui.GartenManagerTheme
import io.github.alpenglowsea.gartenmanager.ui.GartenScreen
import io.github.alpenglowsea.gartenmanager.ui.MeinePflanzenScreen
import io.github.alpenglowsea.gartenmanager.ui.MeinePflanzenZustand
import io.github.alpenglowsea.gartenmanager.ui.OptionenScreen
import io.github.alpenglowsea.gartenmanager.ui.PflanzenKatalogScreen
import io.github.alpenglowsea.gartenmanager.ui.UeberScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GartenManagerTheme {
                val viewModel: GartenViewModel = viewModel()
                val grundstuecke by viewModel.grundstuecke.collectAsState()
                val gaerten by viewModel.gaerten.collectAsState()

                // Welcher Garten ist offen, und ist er im Bearbeitungsmodus?
                var offenerGartenId by rememberSaveable { mutableStateOf<Long?>(null) }
                var bearbeiten by rememberSaveable { mutableStateOf(false) }
                val offenerGarten = gaerten?.firstOrNull { it.id == offenerGartenId }
                // Pflanzenkatalog und "Über GartenManager" (von der Gartenliste aus erreichbar)
                var katalogOffen by rememberSaveable { mutableStateOf(false) }
                var ueberOffen by rememberSaveable { mutableStateOf(false) }
                var optionenOffen by rememberSaveable { mutableStateOf(false) }
                // "Meine Pflanzen" bleibt offen, wenn man von dort in einen Garten springt; Zurück führt dann wieder hinein.
                var meinePflanzenOffen by rememberSaveable { mutableStateOf(false) }
                val meinePflanzenZustand = remember { MeinePflanzenZustand() }
                val bestand by viewModel.bestand.collectAsState()
                val pflanzenImPlan by viewModel.pflanzenImPlan.collectAsState()

                if (optionenOffen && offenerGarten == null) {
                    BackHandler { optionenOffen = false }
                    OptionenScreen(onZurueck = { optionenOffen = false })
                } else if (ueberOffen && offenerGarten == null) {
                    BackHandler { ueberOffen = false }
                    UeberScreen(onZurueck = { ueberOffen = false })
                } else if (meinePflanzenOffen && offenerGarten == null) {
                    BackHandler { meinePflanzenOffen = false }
                    MeinePflanzenScreen(
                        zustand = meinePflanzenZustand,
                        gaerten = gaerten.orEmpty(),
                        imPlan = pflanzenImPlan,
                        bestand = bestand,
                        onSpeichern = viewModel::speichereBestand,
                        onLoeschen = viewModel::loescheBestand,
                        onGartenOeffnen = { id, pflanzeId, gegenstandId ->
                            viewModel.hervorgehobenePflanzeId = pflanzeId
                            viewModel.hervorgehobenerGegenstandId = gegenstandId
                            bearbeiten = false
                            offenerGartenId = id
                        },
                        onPlatzieren = { id, eintrag ->
                            // Garten im Bearbeitungsmodus öffnen; das Setzen der Pflanze ist dort gleich scharf geschaltet.
                            viewModel.vorgemerktePflanze = VorgemerktePflanze(
                                id = eintrag.pflanzeId,
                                name = eintrag.pflanzeName,
                                latein = eintrag.pflanzeLatein,
                                gruppe = eintrag.pflanzeGruppe,
                            )
                            viewModel.hervorgehobenePflanzeId = null
                            viewModel.hervorgehobenerGegenstandId = null
                            bearbeiten = true
                            offenerGartenId = id
                        },
                        onZurueck = { meinePflanzenOffen = false },
                    )
                } else if (katalogOffen && offenerGarten == null) {
                    // Das Zurück-Verhalten innerhalb des Katalogs (Info-Fenster -> Liste) steckt im Katalog selbst.
                    BackHandler { katalogOffen = false }
                    PflanzenKatalogScreen(onZurueck = { katalogOffen = false })
                } else if (offenerGarten != null) {
                    BackHandler {
                        // Zurück aus dem Bearbeiten führt in die Ansicht, aus der Ansicht in die Liste.
                        if (bearbeiten) bearbeiten = false else offenerGartenId = null
                    }
                    val gartenId = offenerGarten.id
                    val flaechen by remember(gartenId) { viewModel.flaechen(gartenId) }
                        .collectAsState(initial = emptyList())
                    val ebenen by remember(gartenId) { viewModel.ebenen(gartenId) }
                        .collectAsState(initial = emptyList())
                    val gegenstaende by remember(gartenId) { viewModel.gegenstaende(gartenId) }
                        .collectAsState(initial = emptyList())
                    val gegenstandspunkte by remember(gartenId) { viewModel.gegenstandspunkte(gartenId) }
                        .collectAsState(initial = emptyList())
                    val punkte by remember(gartenId) { viewModel.punkte(gartenId) }
                        .collectAsState(initial = emptyList())
                    // Beim Betreten oder Verlassen des Bearbeitungsmodus: angefangene Zeichnung
                    // und Rückgängig-Verlauf zurücksetzen.
                    LaunchedEffect(bearbeiten, gartenId) { viewModel.beendeBearbeitung() }
                    // Jeder Garten hat mindestens eine Ebene.
                    LaunchedEffect(gartenId) { viewModel.sichereEbene(gartenId) }

                    GartenScreen(
                        garten = offenerGarten,
                        flaechen = flaechen,
                        ebenen = ebenen,
                        gegenstaende = gegenstaende,
                        gegenstandspunkte = gegenstandspunkte,
                        punkte = punkte,
                        bearbeiten = bearbeiten,
                        viewModel = viewModel,
                        onBearbeiten = { bearbeiten = true },
                        onFertig = { bearbeiten = false },
                        onZurueck = { offenerGartenId = null },
                        onAnsichtGeaendert = { zoom, x, y ->
                            viewModel.speichereAnsicht(gartenId, zoom, x, y)
                        },
                    )
                } else {
                    GartenListeScreen(
                        grundstuecke = grundstuecke,
                        gaerten = gaerten,
                        aufgeklappt = viewModel.aufgeklappt,
                        pflanzenZahl = pflanzenImPlan.size + bestand.sumOf { it.anzahl },
                        artenZahl = (pflanzenImPlan.mapNotNull { it.pflanzeId } + bestand.map { it.pflanzeId }).distinct().size,
                        onSchalteAufgeklappt = viewModel::schalteAufgeklappt,
                        onGartenOeffnen = { id ->
                            bearbeiten = false
                            offenerGartenId = id
                        },
                        onGartenBearbeiten = { id ->
                            bearbeiten = true
                            offenerGartenId = id
                        },
                        onGartenAnlegen = viewModel::legeGartenAn,
                        onGrundstueckAnlegen = viewModel::legeGrundstueckAn,
                        onGartenUmbenennen = viewModel::benenneGartenUm,
                        onGrundstueckUmbenennen = viewModel::benenneGrundstueckUm,
                        onGartenVerschieben = viewModel::verschiebeGarten,
                        onGartenDuplizieren = viewModel::dupliziereGarten,
                        onGartenLoeschen = viewModel::loescheGarten,
                        onGrundstueckAufloesen = viewModel::loeseGrundstueckAuf,
                        onGrundstueckMitGaertenLoeschen = viewModel::loescheGrundstueckMitGaerten,
                        onGaertenOrdnen = viewModel::ordneGaerten,
                        onKatalog = { katalogOffen = true },
                        onMeinePflanzen = { meinePflanzenOffen = true },
                        onUeber = { ueberOffen = true },
                        onOptionen = { optionenOffen = true },
                    )
                }
            }
        }
    }
}
