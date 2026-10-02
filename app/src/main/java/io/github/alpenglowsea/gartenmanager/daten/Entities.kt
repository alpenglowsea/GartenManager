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
    // Maßstab: so viele Skizzeneinheiten sind ein Meter (kommt mit Teilschritt 3a)
    val massstab: Float = 10f,
)

/**
 * Eine Ebene ist ein Behälter für mehrere Flächen und Gegenstände (Teilschritt 3a2). Die
 * Ebenen werden von hinten nach vorn gezeichnet; [reihenfolge] bestimmt den Platz (höhere
 * Zahl = weiter vorn). [name] leer = "Ebene n". [ausgeblendet]: die ganze Ebene wird weder
 * gezeichnet noch angetippt. Wird der Garten gelöscht, verschwinden auch seine Ebenen.
 */
@Entity(
    tableName = "ebene",
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
data class Ebene(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gartenId: Long,
    val name: String? = null,
    val reihenfolge: Int = 0,
    val ausgeblendet: Boolean = false,
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
        ForeignKey(
            entity = Ebene::class,
            parentColumns = ["id"],
            childColumns = ["ebeneId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gartenId"), Index("ebeneId")],
)
data class Flaeche(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gartenId: Long,
    val ebeneId: Long,
    val name: String? = null,
    val oberflaeche: String = "gras",
    val reihenfolge: Int = 0,
    // Gewählte Farbe (ARGB als Zahl); leer = Standardfarbe der Oberfläche (Einsatz ab Teilschritt 3c)
    val farbe: Int? = null,
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

/**
 * Ein Gegenstand (Haus, Auto, Zaun, ...). Die Tabelle wird mit Teilschritt 3a angelegt und
 * ab 3b benutzt. [art] ist ein Schlüssel aus dem Katalog. Lage ([mitteX], [mitteY]) und Größe
 * ([breite], [hoehe]) sind Skizzeneinheiten, der Maßstab des Gartens macht daraus Meter.
 * [drehung] in Grad. [farbe] ist die Hauptfarbe (leer = Standard). [reihenfolge] teilt
 * sich den Zähler mit den Flächen (höhere Zahl = weiter oben).
 */
@Entity(
    tableName = "gegenstand",
    foreignKeys = [
        ForeignKey(
            entity = Garten::class,
            parentColumns = ["id"],
            childColumns = ["gartenId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Ebene::class,
            parentColumns = ["id"],
            childColumns = ["ebeneId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gartenId"), Index("ebeneId")],
)
data class Gegenstand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gartenId: Long,
    val ebeneId: Long,
    val art: String,
    val name: String? = null,
    val mitteX: Float = 0f,
    val mitteY: Float = 0f,
    val breite: Float = 0f,
    val hoehe: Float = 0f,
    val drehung: Float = 0f,
    val farbe: Int? = null,
    val reihenfolge: Int = 0,
)

/**
 * Ein Punkt eines Gebäudes in freier Form (nur dafür). Die Lage ist relativ zur Mitte des
 * Gegenstands (vor der Drehung), gleicher Aufbau wie [Punkt].
 */
@Entity(
    tableName = "gegenstandspunkt",
    foreignKeys = [
        ForeignKey(
            entity = Gegenstand::class,
            parentColumns = ["id"],
            childColumns = ["gegenstandId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gegenstandId")],
)
data class Gegenstandspunkt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gegenstandId: Long,
    val nr: Int,
    val x: Float,
    val y: Float,
    val rund: Boolean = true,
)
