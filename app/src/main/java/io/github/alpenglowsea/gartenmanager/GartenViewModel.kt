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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    // ---- Zeichnen und Bearbeiten (Teilschritte 2c und 2e) ----

    fun flaechen(gartenId: Long): Flow<List<Flaeche>> = dao.flaechen(gartenId)

    fun punkte(gartenId: Long): Flow<List<Punkt>> = dao.punkte(gartenId)

    /** Die gerade angefangene Fläche (Punkte in Skizzenkoordinaten). null = es wird nicht gezeichnet. */
    var zeichnung by mutableStateOf<List<Offset>?>(null)
        private set

    /** Gilt für alle Punkte der angefangenen Fläche: rund (Standard) oder eckig. */
    var zeichnungRund by mutableStateOf(true)
        private set

    /** Gewählte Form, die gerade aufgezogen werden soll. null = keine Form. */
    var formAuswahl by mutableStateOf<Form?>(null)
        private set

    /** Ausrichtungshilfe beim Setzen und Ziehen von Punkten (waagerecht, senkrecht, rechter Winkel). */
    var einrasten by mutableStateOf(true)
        private set

    /** Ausgewählte Fläche und ausgewählter Punkt (nur im Bearbeitungsmodus). */
    var auswahlFlaeche by mutableStateOf<Long?>(null)
        private set
    var auswahlPunkt by mutableStateOf<Long?>(null)
        private set

    // Rückgängig: Abbilder des Gartens vor jeder Änderung. Endet beim Verlassen des Bearbeitungsmodus.
    private class Abbild(val flaechen: List<Flaeche>, val punkte: List<Punkt>)

    private val verlauf = mutableListOf<Abbild>()
    private val sperre = Mutex()
    var anzahlRueckgaengig by mutableIntStateOf(0)
        private set

    fun starteZeichnung() {
        formAuswahl = null
        zeichnung = emptyList()
        zeichnungRund = true
        waehleFlaeche(null)
    }

    fun waehleForm(form: Form) {
        zeichnung = null
        formAuswahl = form
        waehleFlaeche(null)
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

    fun waehleFlaeche(id: Long?) {
        auswahlFlaeche = id
        auswahlPunkt = null
    }

    fun waehlePunkt(flaecheId: Long, punktId: Long) {
        auswahlFlaeche = flaecheId
        auswahlPunkt = punktId
    }

    /**
     * Führt eine Änderung am Garten aus. Vorher wird ein Abbild für "Rückgängig" gemerkt.
     * Alle Änderungen laufen nacheinander (Sperre), damit die Abbilder zur Reihenfolge passen.
     */
    private fun aendere(gartenId: Long, block: suspend () -> Unit) {
        viewModelScope.launch {
            sperre.withLock {
                verlauf.add(Abbild(dao.flaechenListe(gartenId), dao.punkteDesGartens(gartenId)))
                if (verlauf.size > MAX_VERLAUF) verlauf.removeAt(0)
                anzahlRueckgaengig = verlauf.size
                block()
                dao.beruehreGarten(gartenId, jetzt())
            }
        }
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
        aendere(gartenId) {
            val id = dao.legeFlaecheAn(
                gartenId,
                punkte.mapIndexed { nr, o -> Punkt(flaecheId = 0, nr = nr, x = o.x, y = o.y, rund = rund[nr]) },
                jetzt(),
            )
            // Die neue Fläche ist gleich ausgewählt, damit man sie sofort verändern kann.
            waehleFlaeche(id)
        }
    }

    fun verschiebePunkt(gartenId: Long, punktId: Long, x: Float, y: Float) {
        aendere(gartenId) { dao.setzePunktLage(punktId, x, y) }
    }

    fun verschiebeFlaeche(gartenId: Long, flaecheId: Long, dx: Float, dy: Float) {
        aendere(gartenId) { dao.verschiebeFlaeche(flaecheId, dx, dy) }
    }

    /** Fügt einen Punkt als Nummer [nr] in eine Fläche ein und wählt ihn aus. */
    fun fuegePunktEin(gartenId: Long, flaecheId: Long, nr: Int, x: Float, y: Float, rund: Boolean) {
        aendere(gartenId) {
            val id = dao.fuegePunktAn(flaecheId, nr, x, y, rund)
            waehlePunkt(flaecheId, id)
        }
    }

    fun loeschePunkt(gartenId: Long, flaecheId: Long, punktId: Long, nr: Int) {
        auswahlPunkt = null
        aendere(gartenId) { dao.entfernePunkt(flaecheId, punktId, nr) }
    }

    fun setzePunktRund(gartenId: Long, punktId: Long, rund: Boolean) {
        aendere(gartenId) { dao.setzePunktRund(punktId, rund) }
    }

    fun setzeAlleRund(gartenId: Long, flaecheId: Long, rund: Boolean) {
        aendere(gartenId) { dao.setzeAlleRund(flaecheId, rund) }
    }

    fun benenneFlaecheUm(gartenId: Long, flaecheId: Long, name: String) {
        aendere(gartenId) { dao.benenneFlaecheUm(flaecheId, name) }
    }

    /** schritt = +1: eine Ebene nach vorn (oben), -1: eine Ebene nach hinten. */
    fun bewegeFlaecheInReihenfolge(gartenId: Long, flaecheId: Long, schritt: Int) {
        aendere(gartenId) { dao.bewegeInReihenfolge(gartenId, flaecheId, schritt) }
    }

    fun loescheFlaeche(gartenId: Long, flaecheId: Long) {
        waehleFlaeche(null)
        aendere(gartenId) { dao.loescheFlaeche(flaecheId) }
    }

    /** Nimmt die letzte Änderung zurück (solange man im Bearbeitungsmodus ist). */
    fun macheRueckgaengig(gartenId: Long) {
        viewModelScope.launch {
            sperre.withLock {
                if (verlauf.isEmpty()) return@withLock
                val abbild = verlauf.removeAt(verlauf.size - 1)
                anzahlRueckgaengig = verlauf.size
                dao.stelleWiederHer(gartenId, abbild.flaechen, abbild.punkte)
                // Die Auswahl gilt nur weiter, wenn es die Fläche und den Punkt noch gibt.
                val flaecheDa = abbild.flaechen.any { it.id == auswahlFlaeche }
                if (!flaecheDa) {
                    waehleFlaeche(null)
                } else if (abbild.punkte.none { it.id == auswahlPunkt }) {
                    auswahlPunkt = null
                }
            }
        }
    }

    /** Beim Verlassen des Bearbeitungsmodus: angefangene Zeichnung, Auswahl und Rückgängig-Verlauf verwerfen. */
    fun beendeBearbeitung() {
        zeichnung = null
        formAuswahl = null
        waehleFlaeche(null)
        viewModelScope.launch {
            sperre.withLock {
                verlauf.clear()
                anzahlRueckgaengig = 0
            }
        }
    }

    private companion object {
        const val MAX_VERLAUF = 50
    }
}
