package io.github.alpenglowsea.gartenmanager.daten

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GartenDao {

    @Query("SELECT * FROM grundstueck ORDER BY name COLLATE NOCASE")
    abstract fun grundstuecke(): Flow<List<Grundstueck>>

    @Query("SELECT * FROM garten ORDER BY name COLLATE NOCASE")
    abstract fun gaerten(): Flow<List<Garten>>

    @Query("SELECT * FROM garten WHERE id = :id")
    abstract suspend fun gartenMitId(id: Long): Garten?

    @Insert
    abstract suspend fun fuegeGrundstueckEin(grundstueck: Grundstueck): Long

    @Insert
    abstract suspend fun fuegeGartenEin(garten: Garten): Long

    @Query("UPDATE grundstueck SET name = :name WHERE id = :id")
    abstract suspend fun benenneGrundstueckUm(id: Long, name: String)

    @Query("UPDATE garten SET ansichtZoom = :zoom, ansichtX = :x, ansichtY = :y WHERE id = :id")
    abstract suspend fun speichereAnsicht(id: Long, zoom: Float, x: Float, y: Float)

    @Query("UPDATE garten SET name = :name, geaendertAm = :jetzt WHERE id = :id")
    abstract suspend fun benenneGartenUm(id: Long, name: String, jetzt: Long)

    @Query("UPDATE garten SET grundstueckId = :grundstueckId, geaendertAm = :jetzt WHERE id = :id")
    abstract suspend fun verschiebeGarten(id: Long, grundstueckId: Long?, jetzt: Long)

    @Query("DELETE FROM garten WHERE id = :id")
    abstract suspend fun loescheGarten(id: Long)

    @Query("DELETE FROM garten WHERE grundstueckId = :grundstueckId")
    abstract suspend fun loescheGaertenDesGrundstuecks(grundstueckId: Long)

    @Query("DELETE FROM grundstueck WHERE id = :id")
    abstract suspend fun loescheGrundstueck(id: Long)

    /** Löscht das Grundstück und alle seine Gärten in einem Schritt. */
    @Transaction
    open suspend fun loescheGrundstueckMitGaerten(id: Long) {
        loescheGaertenDesGrundstuecks(id)
        loescheGrundstueck(id)
    }

    // ---- Flaechen und Punkte ----

    // ---- Ebenen (Behälter für Flächen und Gegenstände) ----

    @Query("SELECT * FROM ebene WHERE gartenId = :gartenId ORDER BY reihenfolge, id")
    abstract fun ebenen(gartenId: Long): Flow<List<Ebene>>

    @Query("SELECT * FROM ebene WHERE gartenId = :gartenId ORDER BY reihenfolge, id")
    abstract suspend fun ebenenListe(gartenId: Long): List<Ebene>

    @Insert
    abstract suspend fun fuegeEbeneEin(ebene: Ebene): Long

    @Insert
    abstract suspend fun fuegeEbenenEin(ebenen: List<Ebene>)

    @Query("DELETE FROM ebene WHERE gartenId = :gartenId")
    abstract suspend fun loescheEbenenDesGartens(gartenId: Long)

    @Query("DELETE FROM ebene WHERE id = :id")
    abstract suspend fun loescheEbeneZeile(id: Long)

    @Query("UPDATE ebene SET name = :name WHERE id = :id")
    abstract suspend fun benenneEbeneUm(id: Long, name: String?)

    @Query("UPDATE ebene SET sicht = :sicht WHERE id = :id")
    abstract suspend fun setzeEbeneSicht(id: Long, sicht: Int)

    @Query("UPDATE ebene SET reihenfolge = :reihenfolge WHERE id = :id")
    abstract suspend fun setzeEbeneReihenfolge(id: Long, reihenfolge: Int)

    @Query("UPDATE ebene SET reihenfolge = reihenfolge + 1 WHERE gartenId = :gartenId AND reihenfolge > :ueber")
    abstract suspend fun schiebeEbenenNach(gartenId: Long, ueber: Int)

    @Query("UPDATE flaeche SET ebeneId = :nach WHERE ebeneId = :von")
    abstract suspend fun verschiebeFlaechenDerEbene(von: Long, nach: Long)

    @Query("SELECT * FROM flaeche WHERE id = :id")
    abstract suspend fun flaecheMitId(id: Long): Flaeche?

    @Query("UPDATE flaeche SET ebeneId = :ebeneId, reihenfolge = :reihenfolge WHERE id = :id")
    abstract suspend fun setzeFlaecheEbene(id: Long, ebeneId: Long, reihenfolge: Int)

    /**
     * Gibt die gewünschte Ebene zurück, wenn es sie in diesem Garten gibt. Sonst die vorderste.
     * Hat der Garten noch gar keine Ebene, wird die erste angelegt.
     */
    @Transaction
    open suspend fun ebeneFuer(gartenId: Long, gewuenscht: Long?): Long {
        val liste = ebenenListe(gartenId)
        if (gewuenscht != null && liste.any { it.id == gewuenscht }) return gewuenscht
        if (liste.isNotEmpty()) return liste.last().id
        return fuegeEbeneEin(Ebene(gartenId = gartenId, reihenfolge = 0))
    }

    /** Legt eine neue, leere Ebene direkt über [ueber] an (oder ganz vorn, wenn [ueber] fehlt). */
    @Transaction
    open suspend fun legeEbeneAn(gartenId: Long, ueber: Long?): Long {
        val liste = ebenenListe(gartenId)
        val bezug = liste.firstOrNull { it.id == ueber } ?: liste.lastOrNull()
        val neueNummer = if (bezug == null) 0 else bezug.reihenfolge + 1
        if (bezug != null) schiebeEbenenNach(gartenId, bezug.reihenfolge)
        return fuegeEbeneEin(Ebene(gartenId = gartenId, reihenfolge = neueNummer))
    }

    /** Verschiebt eine Ebene um [schritt] Plätze (+1 = weiter nach vorn). */
    @Transaction
    open suspend fun bewegeEbene(gartenId: Long, ebeneId: Long, schritt: Int) {
        val liste = ebenenListe(gartenId).toMutableList()
        val von = liste.indexOfFirst { it.id == ebeneId }
        val nach = von + schritt
        if (von < 0 || nach < 0 || nach >= liste.size) return
        val e = liste.removeAt(von)
        liste.add(nach, e)
        liste.forEachIndexed { index, eb -> setzeEbeneReihenfolge(eb.id, index) }
    }

    /**
     * Löscht eine Ebene, aber nie die letzte. Mit [inhaltBehalten] wandern ihre Flächen in die
     * Ebene dahinter (bei der hintersten in die davor), sonst werden sie mitgelöscht.
     */
    @Transaction
    open suspend fun loescheEbene(gartenId: Long, ebeneId: Long, inhaltBehalten: Boolean) {
        val liste = ebenenListe(gartenId)
        if (liste.size <= 1) return
        val index = liste.indexOfFirst { it.id == ebeneId }
        if (index < 0) return
        if (inhaltBehalten) {
            val ziel = if (index > 0) liste[index - 1] else liste[index + 1]
            verschiebeFlaechenDerEbene(ebeneId, ziel.id)
            verschiebeGegenstaendeDerEbene(ebeneId, ziel.id)
        }
        loescheEbeneZeile(ebeneId) // Was dann noch darin liegt, verschwindet mit (CASCADE).
    }

    /** Verschiebt eine Fläche in die Nachbarebene ([schritt] +1 = nach vorn, -1 = nach hinten). */
    @Transaction
    open suspend fun bewegeFlaecheInNachbarebene(gartenId: Long, flaecheId: Long, schritt: Int) {
        val flaeche = flaecheMitId(flaecheId) ?: return
        val liste = ebenenListe(gartenId)
        val von = liste.indexOfFirst { it.id == flaeche.ebeneId }
        val nach = von + schritt
        if (von < 0 || nach < 0 || nach >= liste.size) return
        setzeFlaecheEbene(flaecheId, liste[nach].id, hoechsteReihenfolge(gartenId) + 1)
    }

    // ---- Flaechen und Punkte ----

    @Query("SELECT * FROM flaeche WHERE gartenId = :gartenId ORDER BY reihenfolge, id")
    abstract fun flaechen(gartenId: Long): Flow<List<Flaeche>>

    @Query(
        "SELECT p.* FROM punkt p INNER JOIN flaeche f ON p.flaecheId = f.id " +
            "WHERE f.gartenId = :gartenId ORDER BY p.flaecheId, p.nr",
    )
    abstract fun punkte(gartenId: Long): Flow<List<Punkt>>

    @Query("SELECT * FROM flaeche WHERE gartenId = :gartenId ORDER BY reihenfolge, id")
    abstract suspend fun flaechenListe(gartenId: Long): List<Flaeche>

    @Query("SELECT * FROM punkt WHERE flaecheId = :flaecheId ORDER BY nr")
    abstract suspend fun punkteListe(flaecheId: Long): List<Punkt>

    /** Höchste Reihenfolgenummer aller Flächen und Gegenstände (gemeinsamer Zähler). */
    @Query(
        "SELECT COALESCE(MAX(m), -1) FROM (SELECT reihenfolge AS m FROM flaeche WHERE gartenId = :gartenId " +
            "UNION ALL SELECT reihenfolge AS m FROM gegenstand WHERE gartenId = :gartenId)",
    )
    abstract suspend fun hoechsteReihenfolge(gartenId: Long): Int

    // ---- Gegenstände ----

    @Query("SELECT * FROM gegenstand WHERE gartenId = :gartenId ORDER BY reihenfolge, id")
    abstract fun gegenstaende(gartenId: Long): Flow<List<Gegenstand>>

    @Query("SELECT * FROM gegenstand WHERE gartenId = :gartenId ORDER BY reihenfolge, id")
    abstract suspend fun gegenstaendeListe(gartenId: Long): List<Gegenstand>

    @Query("SELECT * FROM gegenstand WHERE id = :id")
    abstract suspend fun gegenstandMitId(id: Long): Gegenstand?

    @Insert
    abstract suspend fun fuegeGegenstandEin(gegenstand: Gegenstand): Long

    @Insert
    abstract suspend fun fuegeGegenstaendeEin(gegenstaende: List<Gegenstand>)

    @Query("DELETE FROM gegenstand WHERE id = :id")
    abstract suspend fun loescheGegenstand(id: Long)

    @Query("DELETE FROM gegenstand WHERE gartenId = :gartenId")
    abstract suspend fun loescheGegenstaendeDesGartens(gartenId: Long)

    @Query(
        "UPDATE gegenstand SET mitteX = :mitteX, mitteY = :mitteY, breite = :breite, hoehe = :hoehe, " +
            "drehung = :drehung WHERE id = :id",
    )
    abstract suspend fun setzeGegenstandLage(
        id: Long,
        mitteX: Float,
        mitteY: Float,
        breite: Float,
        hoehe: Float,
        drehung: Float,
    )

    @Query("UPDATE gegenstand SET farbe = :farbe WHERE id = :id")
    abstract suspend fun setzeGegenstandFarbe(id: Long, farbe: Int?)

    @Query("UPDATE flaeche SET farbe = :farbe WHERE id = :id")
    abstract suspend fun setzeFlaecheFarbe(id: Long, farbe: Int?)

    @Query("UPDATE gegenstand SET name = :name WHERE id = :id")
    abstract suspend fun benenneGegenstandUm(id: Long, name: String?)

    @Query("UPDATE gegenstand SET ebeneId = :ebeneId, reihenfolge = :reihenfolge WHERE id = :id")
    abstract suspend fun setzeGegenstandEbene(id: Long, ebeneId: Long, reihenfolge: Int)

    @Query("UPDATE gegenstand SET ebeneId = :nach WHERE ebeneId = :von")
    abstract suspend fun verschiebeGegenstaendeDerEbene(von: Long, nach: Long)

    /** Legt einen Gegenstand in der Ebene [ebeneId] ganz oben an (gibt es sie nicht: vorderste Ebene). */
    @Transaction
    open suspend fun legeGegenstandAn(gartenId: Long, ebeneId: Long?, vorlage: Gegenstand, jetzt: Long): Long {
        val ziel = ebeneFuer(gartenId, ebeneId)
        setzeEbeneSicht(ziel, 0)
        val id = fuegeGegenstandEin(
            vorlage.copy(id = 0, gartenId = gartenId, ebeneId = ziel, reihenfolge = hoechsteReihenfolge(gartenId) + 1),
        )
        beruehreGarten(gartenId, jetzt)
        return id
    }

    /** Dupliziert einen Gegenstand (ganz oben, gleiche Ebene, um [dx]/[dy] versetzt). */
    @Transaction
    open suspend fun dupliziereGegenstand(gartenId: Long, gegenstandId: Long, dx: Float, dy: Float): Long? {
        val g = gegenstandMitId(gegenstandId) ?: return null
        return fuegeGegenstandEin(
            g.copy(
                id = 0,
                mitteX = g.mitteX + dx,
                mitteY = g.mitteY + dy,
                reihenfolge = hoechsteReihenfolge(gartenId) + 1,
            ),
        )
    }

    /** Verschiebt einen Gegenstand in die Nachbarebene ([schritt] +1 = nach vorn, -1 = nach hinten). */
    @Transaction
    open suspend fun bewegeGegenstandInNachbarebene(gartenId: Long, gegenstandId: Long, schritt: Int) {
        val g = gegenstandMitId(gegenstandId) ?: return
        val liste = ebenenListe(gartenId)
        val von = liste.indexOfFirst { it.id == g.ebeneId }
        val nach = von + schritt
        if (von < 0 || nach < 0 || nach >= liste.size) return
        setzeGegenstandEbene(gegenstandId, liste[nach].id, hoechsteReihenfolge(gartenId) + 1)
    }

    @Insert
    abstract suspend fun fuegeFlaecheEin(flaeche: Flaeche): Long

    @Insert
    abstract suspend fun fuegePunkteEin(punkte: List<Punkt>)

    @Query("DELETE FROM flaeche WHERE id = :id")
    abstract suspend fun loescheFlaeche(id: Long)

    @Query("UPDATE garten SET geaendertAm = :jetzt WHERE id = :id")
    abstract suspend fun beruehreGarten(id: Long, jetzt: Long)

    @Query(
        "SELECT p.* FROM punkt p INNER JOIN flaeche f ON p.flaecheId = f.id " +
            "WHERE f.gartenId = :gartenId ORDER BY p.flaecheId, p.nr",
    )
    abstract suspend fun punkteDesGartens(gartenId: Long): List<Punkt>

    @Query("UPDATE punkt SET x = :x, y = :y WHERE id = :id")
    abstract suspend fun setzePunktLage(id: Long, x: Float, y: Float)

    @Query("UPDATE punkt SET x = x + :dx, y = y + :dy WHERE flaecheId = :flaecheId")
    abstract suspend fun verschiebeFlaeche(flaecheId: Long, dx: Float, dy: Float)

    @Query("UPDATE punkt SET rund = :rund WHERE id = :id")
    abstract suspend fun setzePunktRund(id: Long, rund: Boolean)

    @Query("UPDATE punkt SET rund = :rund WHERE flaecheId = :flaecheId")
    abstract suspend fun setzeAlleRund(flaecheId: Long, rund: Boolean)

    @Query("UPDATE punkt SET nr = nr + 1 WHERE flaecheId = :flaecheId AND nr >= :ab")
    abstract suspend fun schiebePunkteNach(flaecheId: Long, ab: Int)

    @Query("UPDATE punkt SET nr = nr - 1 WHERE flaecheId = :flaecheId AND nr > :nach")
    abstract suspend fun schliesseLuecke(flaecheId: Long, nach: Int)

    @Query("DELETE FROM punkt WHERE id = :id")
    abstract suspend fun loeschePunkt(id: Long)

    @Query("SELECT COUNT(*) FROM punkt WHERE flaecheId = :flaecheId")
    abstract suspend fun zaehlePunkte(flaecheId: Long): Int

    @Insert
    abstract suspend fun fuegePunktEin(punkt: Punkt): Long

    @Insert
    abstract suspend fun fuegeFlaechenEin(flaechen: List<Flaeche>)

    @Query("UPDATE flaeche SET oberflaeche = :oberflaeche WHERE id = :id")
    abstract suspend fun setzeOberflaeche(id: Long, oberflaeche: String)

    @Query("UPDATE flaeche SET name = :name WHERE id = :id")
    abstract suspend fun benenneFlaecheUm(id: Long, name: String)

    @Query("UPDATE flaeche SET reihenfolge = :reihenfolge WHERE id = :id")
    abstract suspend fun setzeReihenfolge(id: Long, reihenfolge: Int)

    @Query("DELETE FROM flaeche WHERE gartenId = :gartenId")
    abstract suspend fun loescheFlaechenDesGartens(gartenId: Long)

    /** Fügt einen Punkt an Stelle [nr] ein; die folgenden Punkte rücken nach. */
    @Transaction
    open suspend fun fuegePunktAn(flaecheId: Long, nr: Int, x: Float, y: Float, rund: Boolean): Long {
        schiebePunkteNach(flaecheId, nr)
        return fuegePunktEin(Punkt(flaecheId = flaecheId, nr = nr, x = x, y = y, rund = rund))
    }

    /** Entfernt einen Punkt, aber nie unter drei Punkte pro Fläche. */
    @Transaction
    open suspend fun entfernePunkt(flaecheId: Long, punktId: Long, nr: Int) {
        if (zaehlePunkte(flaecheId) <= 3) return
        loeschePunkt(punktId)
        schliesseLuecke(flaecheId, nr)
    }

    /**
     * Stellt den Zustand aller Ebenen, Flächen, Punkte und Gegenstände eines Gartens aus einem
     * Abbild wieder her.
     */
    @Transaction
    open suspend fun stelleWiederHer(
        gartenId: Long,
        ebenen: List<Ebene>,
        flaechen: List<Flaeche>,
        punkte: List<Punkt>,
        gegenstaende: List<Gegenstand>,
    ) {
        loescheEbenenDesGartens(gartenId) // nimmt wegen CASCADE auch alle Flächen, Punkte und Gegenstände mit
        loescheFlaechenDesGartens(gartenId)
        loescheGegenstaendeDesGartens(gartenId)
        fuegeEbenenEin(ebenen)
        fuegeFlaechenEin(flaechen)
        fuegePunkteEin(punkte)
        fuegeGegenstaendeEin(gegenstaende)
        beruehreGarten(gartenId, System.currentTimeMillis())
    }

    /**
     * Legt eine neue Fläche in der Ebene [ebeneId] ganz oben an (gibt es sie nicht, in der
     * vordersten Ebene). Die Punkte kommen mit flaecheId = 0 herein.
     */
    @Transaction
    open suspend fun legeFlaecheAn(gartenId: Long, ebeneId: Long?, punkte: List<Punkt>, jetzt: Long): Long {
        val ziel = ebeneFuer(gartenId, ebeneId)
        setzeEbeneSicht(ziel, 0) // Wer in eine Ebene zeichnet, soll das Ergebnis auch sehen.
        val flaecheId = fuegeFlaecheEin(
            Flaeche(gartenId = gartenId, ebeneId = ziel, reihenfolge = hoechsteReihenfolge(gartenId) + 1),
        )
        fuegePunkteEin(punkte.map { it.copy(flaecheId = flaecheId) })
        beruehreGarten(gartenId, jetzt)
        return flaecheId
    }

    /** Legt eine Kopie eines Gartens samt aller Flaechen und Punkte an. */
    @Transaction
    open suspend fun dupliziereGarten(id: Long, jetzt: Long, zusatz: String): Long? {
        val original = gartenMitId(id) ?: return null
        val kopie = original.copy(
            id = 0,
            name = original.name + zusatz,
            angelegtAm = jetzt,
            geaendertAm = jetzt,
        )
        val kopieId = fuegeGartenEin(kopie)
        // Ebenen zuerst kopieren und merken, welche alte Ebene zu welcher neuen gehört.
        val neueEbenen = HashMap<Long, Long>()
        for (ebene in ebenenListe(id)) {
            neueEbenen[ebene.id] = fuegeEbeneEin(ebene.copy(id = 0, gartenId = kopieId))
        }
        for (flaeche in flaechenListe(id)) {
            val ebeneZiel = neueEbenen[flaeche.ebeneId] ?: ebeneFuer(kopieId, null)
            val neueFlaecheId = fuegeFlaecheEin(flaeche.copy(id = 0, gartenId = kopieId, ebeneId = ebeneZiel))
            fuegePunkteEin(
                punkteListe(flaeche.id).map { it.copy(id = 0, flaecheId = neueFlaecheId) },
            )
        }
        for (g in gegenstaendeListe(id)) {
            val ebeneZiel = neueEbenen[g.ebeneId] ?: ebeneFuer(kopieId, null)
            fuegeGegenstandEin(g.copy(id = 0, gartenId = kopieId, ebeneId = ebeneZiel))
        }
        return kopieId
    }
}
