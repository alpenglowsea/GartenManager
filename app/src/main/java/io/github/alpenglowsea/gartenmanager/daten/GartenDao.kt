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

    /**
     * Legt eine Kopie eines Gartens an (gleiches Grundstück, Name mit Zusatz).
     * Ab Teilschritt 2c kopiert das auch die Flächen und Punkte.
     */
    @Transaction
    open suspend fun dupliziereGarten(id: Long, jetzt: Long, zusatz: String): Long? {
        val original = gartenMitId(id) ?: return null
        val kopie = original.copy(
            id = 0,
            name = original.name + zusatz,
            angelegtAm = jetzt,
            geaendertAm = jetzt,
        )
        return fuegeGartenEin(kopie)
    }
}
