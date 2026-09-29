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

/**
 * Eine gezeichnete Fläche (Beet, Rasen, Terrasse, ...). Wird der Garten gelöscht,
 * verschwinden auch seine Flächen (CASCADE). Wer oben liegt, bestimmt [reihenfolge]
 * (höhere Zahl = weiter oben). [oberflaeche] ist ein Schlüssel wie "gras"; das
 * Aussehen kommt aus dem Programm, nicht aus der Datenbank.
 */
@Entity(
    tableName = "flaeche",
    foreignKeys = [
        ForeignKey(
            entity = Garten::class,
            parentColumns = ["id"],
            childColumns = ["gartenId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gartenId")],
)
data class Flaeche(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gartenId: Long,
    val name: String? = null,
    val oberflaeche: String = "gras",
    val reihenfolge: Int = 0,
)

/**
 * Ein Punkt am Rand einer Fläche. [nr] ist die Zeichenreihenfolge (0, 1, 2, ...).
 * X und Y sind Skizzeneinheiten (keine Meter). [rund] = Kurve läuft weich durch den
 * Punkt, sonst ist der Punkt eine Ecke.
 */
@Entity(
    tableName = "punkt",
    foreignKeys = [
        ForeignKey(
            entity = Flaeche::class,
            parentColumns = ["id"],
            childColumns = ["flaecheId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("flaecheId")],
)
data class Punkt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flaecheId: Long,
    val nr: Int,
    val x: Float,
    val y: Float,
    val rund: Boolean = true,
)
