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
import io.github.alpenglowsea.gartenmanager.ui.PflanzenTestScreen

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
                // Vorläufiger Testbildschirm für die Pflanzen-Grunddaten (Teilschritt 4a)
                var pflanzenTest by rememberSaveable { mutableStateOf(false) }

                if (pflanzenTest && offenerGarten == null) {
                    BackHandler { pflanzenTest = false }
                    PflanzenTestScreen(onZurueck = { pflanzenTest = false })
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
                        onPflanzenTest = { pflanzenTest = true },
                    )
                }
            }
        }
    }
}
