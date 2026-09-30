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

    @Query("SELECT COALESCE(MAX(reihenfolge), -1) FROM flaeche WHERE gartenId = :gartenId")
    abstract suspend fun hoechsteReihenfolge(gartenId: Long): Int

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

    /** Verschiebt eine Fläche um [schritt] Plätze in der Reihenfolge (+1 = weiter nach oben). */
    @Transaction
    open suspend fun bewegeInReihenfolge(gartenId: Long, flaecheId: Long, schritt: Int) {
        val liste = flaechenListe(gartenId).toMutableList()
        val von = liste.indexOfFirst { it.id == flaecheId }
        val nach = von + schritt
        if (von < 0 || nach < 0 || nach >= liste.size) return
        val f = liste.removeAt(von)
        liste.add(nach, f)
        liste.forEachIndexed { index, fl -> setzeReihenfolge(fl.id, index) }
    }

    /** Stellt den Zustand aller Flächen und Punkte eines Gartens aus einem Abbild wieder her. */
    @Transaction
    open suspend fun stelleWiederHer(gartenId: Long, flaechen: List<Flaeche>, punkte: List<Punkt>) {
        loescheFlaechenDesGartens(gartenId)
        fuegeFlaechenEin(flaechen)
        fuegePunkteEin(punkte)
        beruehreGarten(gartenId, System.currentTimeMillis())
    }

    /** Legt eine neue Fläche ganz oben an. Die Punkte kommen mit flaecheId = 0 herein. */
    @Transaction
    open suspend fun legeFlaecheAn(gartenId: Long, punkte: List<Punkt>, jetzt: Long): Long {
        val flaecheId = fuegeFlaecheEin(
            Flaeche(gartenId = gartenId, reihenfolge = hoechsteReihenfolge(gartenId) + 1),
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
        for (flaeche in flaechenListe(id)) {
            val neueFlaecheId = fuegeFlaecheEin(flaeche.copy(id = 0, gartenId = kopieId))
            fuegePunkteEin(
                punkteListe(flaeche.id).map { it.copy(id = 0, flaecheId = neueFlaecheId) },
            )
        }
        return kopieId
    }
}
