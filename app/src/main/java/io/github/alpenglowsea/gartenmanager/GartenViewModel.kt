package io.github.alpenglowsea.gartenmanager

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Grundstueck
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Verbindet die Datenbank mit der Oberfläche. "null" heißt: wird noch geladen.
 * Alle Änderungen werden sofort gespeichert (Entscheidung, siehe GARTENPLAN.md).
 */
class GartenViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MeineDatenDb.holen(application).gartenDao()

    val grundstuecke: StateFlow<List<Grundstueck>?> =
        dao.grundstuecke().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gaerten: StateFlow<List<Garten>?> =
        dao.gaerten().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Welche Grundstücke in der Liste aufgeklappt sind (nur für die laufende Sitzung). */
    var aufgeklappt by mutableStateOf(setOf<Long>())
        private set

    fun schalteAufgeklappt(grundstueckId: Long) {
        aufgeklappt = if (grundstueckId in aufgeklappt) {
            aufgeklappt - grundstueckId
        } else {
            aufgeklappt + grundstueckId
        }
    }

    private fun jetzt(): Long = System.currentTimeMillis()

    fun legeGartenAn(name: String, grundstueckId: Long?) {
        viewModelScope.launch {
            val t = jetzt()
            dao.fuegeGartenEin(
                Garten(grundstueckId = grundstueckId, name = name.trim(), angelegtAm = t, geaendertAm = t),
            )
            if (grundstueckId != null) aufgeklappt = aufgeklappt + grundstueckId
        }
    }

    fun legeGrundstueckAn(name: String) {
        viewModelScope.launch {
            val id = dao.fuegeGrundstueckEin(Grundstueck(name = name.trim()))
            aufgeklappt = aufgeklappt + id
        }
    }

    fun benenneGartenUm(id: Long, name: String) {
        viewModelScope.launch { dao.benenneGartenUm(id, name.trim(), jetzt()) }
    }

    fun benenneGrundstueckUm(id: Long, name: String) {
        viewModelScope.launch { dao.benenneGrundstueckUm(id, name.trim()) }
    }

    fun verschiebeGarten(id: Long, grundstueckId: Long?) {
        viewModelScope.launch {
            dao.verschiebeGarten(id, grundstueckId, jetzt())
            if (grundstueckId != null) aufgeklappt = aufgeklappt + grundstueckId
        }
    }

    fun dupliziereGarten(id: Long, zusatz: String) {
        viewModelScope.launch { dao.dupliziereGarten(id, jetzt(), zusatz) }
    }

    /** Speichert die letzte Ansicht, ohne "zuletzt geaendert" zu veraendern. */
    fun speichereAnsicht(id: Long, zoom: Float, x: Float, y: Float) {
        viewModelScope.launch { dao.speichereAnsicht(id, zoom, x, y) }
    }

    fun loescheGarten(id: Long) {
        viewModelScope.launch { dao.loescheGarten(id) }
    }

    /** Löscht nur die Gruppe. Die Gärten bleiben als einzelne Gärten erhalten. */
    fun loeseGrundstueckAuf(id: Long) {
        viewModelScope.launch { dao.loescheGrundstueck(id) }
    }

    fun loescheGrundstueckMitGaerten(id: Long) {
        viewModelScope.launch { dao.loescheGrundstueckMitGaerten(id) }
    }
}
