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
 * Zahl = weiter vorn). [name] leer = "Ebene n". [sicht]: 0 = sichtbar, 1 = halbtransparent
 * (noch antippbar), 2 = ausgeblendet (weder gezeichnet noch angetippt). Wird der Garten gelöscht, verschwinden auch seine Ebenen.
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
    val sicht: Int = 0,
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
 *
 * Eine im Garten gesetzte Pflanze ist ein Gegenstand mit [art] = [ART_PFLANZE] (Teilschritt 4d). Breite
 * und Höhe sind dann gleich (der Durchmesser). [pflanzeId] ist die Nummer in den Grunddaten; Name,
 * lateinischer Name und Gruppe sind eine Notfall-Kopie, damit die Pflanze auch dann sinnvoll
 * erscheint, wenn die Nummer in den Grunddaten einmal fehlt. [name] bleibt der eigene Name des Nutzers.
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
    val pflanzeId: Long? = null,
    val pflanzeName: String? = null,
    val pflanzeLatein: String? = null,
    val pflanzeGruppe: String? = null,
    /** Pflanzdatum als Tag seit 01.01.1970 (LocalDate.toEpochDay), leer = unbekannt. */
    val pflanzdatum: Long? = null,
    /** Eigene Notiz zur gesetzten Pflanze (höchstens 500 Zeichen), leer = keine. */
    val notiz: String? = null,
)

/** Schlüssel in [Gegenstand.art] für eine gesetzte Pflanze. */
const val ART_PFLANZE = "pflanze"

/**
 * Eigene Pflanzen bekommen Nummern ab dieser Zahl (EIGENE_BASIS + Nummer in der Tabelle eigene_pflanze).
 * So teilen sie sich mit den Pflanzen der Grunddaten einen Nummernraum und können nie kollidieren.
 */
const val EIGENE_BASIS = 1_000_000L

fun istEigenePflanze(nummer: Long): Boolean = nummer >= EIGENE_BASIS

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

/**
 * Persönlicher Bildausschnitt einer Pflanze aus den Grunddaten: Mittelpunkt (0 bis 1, vom oberen
 * linken Eck des Bildes aus) und Zoom (1 = größtmöglicher Ausschnitt). Gilt für Kachel und Info-Fenster.
 * Fehlt die Zeile, wird aus der Mitte ausgeschnitten.
 */
@Entity(tableName = "bild_ausschnitt")
data class BildAusschnitt(
    @PrimaryKey val pflanzeId: Long,
    val mitteX: Float,
    val mitteY: Float,
    val zoom: Float,
)

/** Eine vom Nutzer angelegte Pflanze, die es im Katalog nicht gibt (Teilschritt 4e). Die Texte stehen in [EigeneAngabe]. */
@Entity(tableName = "eigene_pflanze")
data class EigenePflanze(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lateinisch: String = "",
    val gattung: String = "",
    val gruppe: String,
    val angelegtAm: Long,
)

/**
 * Ein eigener Text zu einem Abschnitt einer Pflanze: bei einer Pflanze aus den Grunddaten die "Meine Notiz",
 * bei einer eigenen Pflanze der Inhalt des Abschnitts. [pflanzeId] ist die Pflanzennummer (bei eigenen Pflanzen
 * mit [EIGENE_BASIS]).
 */
@Entity(tableName = "eigene_angabe", primaryKeys = ["pflanzeId", "abschnitt"])
data class EigeneAngabe(
    val pflanzeId: Long,
    val abschnitt: String,
    val text: String,
    val geaendertAm: Long,
)

/** Ein zugeklappter Abschnitt im Info-Fenster einer Pflanze. */
@Entity(tableName = "merker", primaryKeys = ["pflanzeId", "abschnitt"])
data class MerkerZeile(
    val pflanzeId: Long,
    val abschnitt: String,
)

