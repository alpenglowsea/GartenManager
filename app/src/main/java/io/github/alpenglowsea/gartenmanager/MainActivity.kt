package io.github.alpenglowsea.gartenmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alpenglowsea.gartenmanager.ui.GartenListeScreen
import io.github.alpenglowsea.gartenmanager.ui.GartenManagerTheme
import io.github.alpenglowsea.gartenmanager.ui.GartenPlatzhalterScreen

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

                if (offenerGarten != null) {
                    BackHandler {
                        // Zurück aus dem Bearbeiten führt in die Ansicht, aus der Ansicht in die Liste.
                        if (bearbeiten) bearbeiten = false else offenerGartenId = null
                    }
                    GartenPlatzhalterScreen(
                        garten = offenerGarten,
                        bearbeiten = bearbeiten,
                        onBearbeiten = { bearbeiten = true },
                        onFertig = { bearbeiten = false },
                        onZurueck = { offenerGartenId = null },
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
                    )
                }
            }
        }
    }
}
