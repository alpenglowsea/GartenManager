package io.github.alpenglowsea.gartenmanager.daten

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Ein Grundstück fasst optional mehrere Gärten zusammen. */
@Entity(tableName = "grundstueck")
data class Grundstueck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * Ein Garten ist ein eigener Gartenplan. Gehört er zu keinem Grundstück, ist
 * [grundstueckId] leer. Wird ein Grundstück gelöscht, bleibt der Garten erhalten
 * und verliert nur die Zuordnung (SET_NULL).
 */
@Entity(
    tableName = "garten",
    foreignKeys = [
        ForeignKey(
            entity = Grundstueck::class,
            parentColumns = ["id"],
            childColumns = ["grundstueckId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("grundstueckId")],
)
data class Garten(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grundstueckId: Long? = null,
    val name: String,
    val angelegtAm: Long,
    val geaendertAm: Long,
    // Letzte Ansicht (kommt mit Teilschritt 2b zum Einsatz)
    val ansichtZoom: Float = 1f,
    val ansichtX: Float = 0f,
    val ansichtY: Float = 0f,
)
