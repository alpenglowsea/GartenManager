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
