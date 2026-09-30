package io.github.alpenglowsea.gartenmanager

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alpenglowsea.gartenmanager.daten.Flaeche
import io.github.alpenglowsea.gartenmanager.daten.Garten
import io.github.alpenglowsea.gartenmanager.daten.Grundstueck
import io.github.alpenglowsea.gartenmanager.daten.MeineDatenDb
import io.github.alpenglowsea.gartenmanager.daten.Punkt
import io.github.alpenglowsea.gartenmanager.ui.Form
import kotlinx.coroutines.flow.Flow
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

    // ---- Zeichnen (Teilschritt 2c) ----

    fun flaechen(gartenId: Long): Flow<List<Flaeche>> = dao.flaechen(gartenId)

    fun punkte(gartenId: Long): Flow<List<Punkt>> = dao.punkte(gartenId)

    /** Die gerade angefangene Fläche (Punkte in Skizzenkoordinaten). null = es wird nicht gezeichnet. */
    var zeichnung by mutableStateOf<List<Offset>?>(null)
        private set

    /** Gilt für alle Punkte der angefangenen Fläche: rund (Standard) oder eckig. */
    var zeichnungRund by mutableStateOf(true)
        private set

    // Im Bearbeitungsmodus angelegte Flächen (für "Rückgängig"). Endet beim Verlassen des Modus.
    private val angelegteFlaechen = mutableListOf<Long>()
    var anzahlRueckgaengig by mutableIntStateOf(0)
        private set

    /** Gewählte Form, die gerade aufgezogen werden soll. null = keine Form. */
    var formAuswahl by mutableStateOf<Form?>(null)
        private set

    /** Ausrichtungshilfe beim Setzen von Punkten (waagerecht, senkrecht, rechter Winkel). */
    var einrasten by mutableStateOf(true)
        private set

    fun starteZeichnung() {
        formAuswahl = null
        zeichnung = emptyList()
        zeichnungRund = true
    }

    fun waehleForm(form: Form) {
        zeichnung = null
        formAuswahl = form
    }

    fun brichFormAb() {
        formAuswahl = null
    }

    fun schalteEinrasten() {
        einrasten = !einrasten
    }

    fun setzePunkt(punkt: Offset) {
        val aktuell = zeichnung ?: return
        zeichnung = aktuell + punkt
    }

    fun entferneLetztenPunkt() {
        val aktuell = zeichnung ?: return
        zeichnung = aktuell.dropLast(1)
    }

    fun schalteRund() {
        zeichnungRund = !zeichnungRund
    }

    fun brichZeichnungAb() {
        zeichnung = null
    }

    /** Schließt die angefangene Fläche (Tipp auf den ersten Punkt, ab drei Punkten). */
    fun schliesseFlaecheAb(gartenId: Long) {
        val punkte = zeichnung ?: return
        if (punkte.size < 3) return
        val rund = zeichnungRund
        zeichnung = null
        speichereFlaeche(gartenId, punkte, List(punkte.size) { rund })
    }

    /** Legt eine aufgezogene Form als Fläche an. */
    fun legeFormAn(gartenId: Long, punkte: List<Offset>, rund: List<Boolean>) {
        formAuswahl = null
        if (punkte.size < 3) return
        speichereFlaeche(gartenId, punkte, rund)
    }

    private fun speichereFlaeche(gartenId: Long, punkte: List<Offset>, rund: List<Boolean>) {
        viewModelScope.launch {
            val id = dao.legeFlaecheAn(
                gartenId,
                punkte.mapIndexed { nr, o -> Punkt(flaecheId = 0, nr = nr, x = o.x, y = o.y, rund = rund[nr]) },
                jetzt(),
            )
            angelegteFlaechen.add(id)
            anzahlRueckgaengig = angelegteFlaechen.size
        }
    }

    /** Nimmt die zuletzt angelegte Fläche zurück (solange man im Bearbeitungsmodus ist). */
    fun macheFlaecheRueckgaengig() {
        if (angelegteFlaechen.isEmpty()) return
        val id = angelegteFlaechen.removeAt(angelegteFlaechen.size - 1)
        anzahlRueckgaengig = angelegteFlaechen.size
        viewModelScope.launch { dao.loescheFlaeche(id) }
    }

    /** Beim Verlassen des Bearbeitungsmodus: angefangene Zeichnung und Rückgängig-Verlauf verwerfen. */
    fun beendeBearbeitung() {
        zeichnung = null
        formAuswahl = null
        angelegteFlaechen.clear()
        anzahlRueckgaengig = 0
    }
}
