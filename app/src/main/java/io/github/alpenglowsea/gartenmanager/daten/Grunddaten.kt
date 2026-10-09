package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.Normalizer

/** Kopfdaten der Grunddaten-Datei. */
data class GrunddatenInfo(val version: String, val testdaten: Boolean, val anzahlPflanzen: Int)

data class GruppeMitAnzahl(val id: Long, val name: String, val anzahl: Int)

data class PflanzeTreffer(
    val id: Long,
    val hauptname: String,
    val lateinisch: String,
    val hauptgruppe: String,
    /** Der Name, über den die Pflanze gefunden wurde (nur wenn er vom Hauptnamen abweicht). */
    val gefundenUeber: String?,
    /** Dateiname des Bildes in assets/bilder, oder null. */
    val bild: String? = null,
)

/** Das Foto einer Pflanze samt Nachweis (Urheber, Lizenz, Quelle). */
data class BildInfo(
    val datei: String,
    val urheber: String?,
    val lizenz: String,
    val lizenzUrl: String?,
    val seite: String?,
    val titel: String?,
    val herkunft: String?,
    val abruf: String?,
)

data class QuelleInfo(
    val schluessel: String,
    val art: String,
    val titel: String,
    val adresse: String?,
    val version: String?,
    val lizenz: String?,
    val abrufdatum: String?,
    val aenderung: String?,
)

data class NameInfo(val name: String, val art: String)

data class AngabeInfo(val abschnitt: String, val text: String, val quelle: String, val auszug: Boolean)

/** Alles, was das Info-Fenster zu einer Pflanze zeigt. */
data class PflanzeDetail(
    val id: Long,
    val hauptname: String,
    val lateinisch: String,
    val gattung: String,
    val wikipedia: String?,
    val wikidata: String?,
    val hauptgruppe: String,
    val gruppen: List<String>,
    val namen: List<NameInfo>,
    val angaben: List<AngabeInfo>,
    /** Quellen der Angaben und Namen dieser Pflanze, in der Reihenfolge des ersten Auftretens. */
    val quellen: List<QuelleInfo>,
    val bild: BildInfo? = null,
)

/** Eine Zeile der Quellenübersicht: Art (zum Beispiel Wikipedia), Lizenz und Anzahl. */
data class QuellenZaehlung(val art: String, val lizenz: String?, val anzahl: Int)

/**
 * Lesezugriff auf die mitgelieferte Grunddaten-Datei ("Datei 1").
 *
 * Die Datei liegt als Asset in der App und wird beim ersten Start (und nach jedem Update,
 * das eine neue Version mitbringt) in den App-Speicher kopiert. Sie wird nur gelesen.
 * Die eigenen Daten des Nutzers liegen in einer anderen Datei und werden nie angefasst.
 */
object Grunddaten {
    private const val DATEI = "grunddaten.db"
    private const val VERSIONSDATEI = "grunddaten.version"
    private const val PREFS = "grunddaten"
    private const val PREFS_VERSION = "version"

    private var db: SQLiteDatabase? = null

    /**
     * Muss exakt so arbeiten wie `normalisiere` in tools/baue_grunddaten.py:
     * klein, ä→ae ö→oe ü→ue ß→ss, übrige Akzente weg, Bindestrich→Leerzeichen, Leerzeichen zusammenfassen.
     */
    fun normalisiere(text: String): String {
        var s = text.lowercase()
        s = s.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = s.filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        s = s.replace('-', ' ')
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /** Öffnet die Datei; kopiert sie vorher aus den Assets, falls nötig. */
    @Synchronized
    private fun oeffne(context: Context): SQLiteDatabase {
        db?.let { if (it.isOpen) return it }
        val app = context.applicationContext
        val ziel = File(app.filesDir, DATEI)
        val mitgeliefert = app.assets.open(VERSIONSDATEI).use { it.readBytes().toString(Charsets.UTF_8).trim() }
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!ziel.exists() || prefs.getString(PREFS_VERSION, null) != mitgeliefert) {
            val temp = File(app.filesDir, "$DATEI.neu")
            app.assets.open(DATEI).use { eingang -> temp.outputStream().use { eingang.copyTo(it) } }
            if (ziel.exists() && !ziel.delete()) error("Alte Grunddaten konnten nicht ersetzt werden")
            if (!temp.renameTo(ziel)) error("Neue Grunddaten konnten nicht abgelegt werden")
            prefs.edit().putString(PREFS_VERSION, mitgeliefert).apply()
        }
        val neu = SQLiteDatabase.openDatabase(ziel.path, null, SQLiteDatabase.OPEN_READONLY)
        db = neu
        return neu
    }

    fun info(context: Context): GrunddatenInfo {
        val d = oeffne(context)
        fun meta(k: String): String? =
            d.rawQuery("SELECT wert FROM meta WHERE schluessel = ?", arrayOf(k)).use { if (it.moveToFirst()) it.getString(0) else null }
        val anzahl = d.rawQuery("SELECT COUNT(*) FROM pflanze WHERE veraltet = 0", null).use { it.moveToFirst(); it.getInt(0) }
        return GrunddatenInfo(meta("version") ?: "?", meta("testdaten") == "1", anzahl)
    }

    fun gruppen(context: Context): List<GruppeMitAnzahl> {
        val d = oeffne(context)
        val sql = "SELECT g.id, g.name, COUNT(pg.pflanze_id) FROM gruppe g " +
            "LEFT JOIN pflanze_gruppe pg ON pg.gruppe_id = g.id " +
            "AND pg.pflanze_id IN (SELECT id FROM pflanze WHERE veraltet = 0) " +
            "GROUP BY g.id, g.name ORDER BY g.id"
        return d.rawQuery(sql, null).use { c ->
            val liste = mutableListOf<GruppeMitAnzahl>()
            while (c.moveToNext()) liste += GruppeMitAnzahl(c.getLong(0), c.getString(1), c.getInt(2))
            liste
        }
    }

    /** Sucht in allen Namen (Haupt-, umgangssprachlich, lateinisch). Mehrere Gruppen gelten als "oder". */
    fun suche(context: Context, text: String, gruppenIds: Set<Long>): List<PflanzeTreffer> {
        val d = oeffne(context)
        val q = normalisiere(text).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val args = mutableListOf<String>()
        val mitText = q.isNotEmpty()
        val gefunden = if (mitText) {
            args += "%$q%"
            "(SELECT n.name FROM name n WHERE n.pflanze_id = p.id AND n.suchtext LIKE ? ESCAPE '\\' LIMIT 1)"
        } else "NULL"
        val sb = StringBuilder(
            "SELECT p.id, p.hauptname, p.lateinisch, " +
                "(SELECT g.name FROM pflanze_gruppe pg JOIN gruppe g ON g.id = pg.gruppe_id " +
                "WHERE pg.pflanze_id = p.id AND pg.haupt = 1), $gefunden, " +
                "(SELECT b.datei FROM bild b WHERE b.pflanze_id = p.id) " +
                "FROM pflanze p WHERE p.veraltet = 0"
        )
        if (mitText) {
            sb.append(" AND p.id IN (SELECT pflanze_id FROM name WHERE suchtext LIKE ? ESCAPE '\\')")
            args += "%$q%"
        }
        if (gruppenIds.isNotEmpty()) {
            sb.append(" AND p.id IN (SELECT pflanze_id FROM pflanze_gruppe WHERE gruppe_id IN (")
            sb.append(gruppenIds.joinToString(",") { "?" }).append("))")
            gruppenIds.forEach { args += it.toString() }
        }
        sb.append(" ORDER BY p.sortname, p.lateinisch")
        return d.rawQuery(sb.toString(), args.toTypedArray()).use { c ->
            val liste = mutableListOf<PflanzeTreffer>()
            while (c.moveToNext()) {
                val name = c.getString(1)
                val lat = c.getString(2)
                val ueber = c.getString(4)?.takeIf { it != name && it != lat }
                liste += PflanzeTreffer(c.getLong(0), name, lat, c.getString(3) ?: "", ueber, c.getString(5))
            }
            liste
        }
    }

    /** Alle Angaben zu einer Pflanze für das Info-Fenster; null, wenn es die Nummer nicht gibt. */
    fun pflanze(context: Context, id: Long): PflanzeDetail? {
        val d = oeffne(context)
        val arg = arrayOf(id.toString())
        val kopf = d.rawQuery(
            "SELECT hauptname, lateinisch, gattung, wikipedia, wikidata FROM pflanze WHERE id = ?", arg,
        ).use { c ->
            if (!c.moveToFirst()) return null
            arrayOf(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))
        }
        val gruppen = mutableListOf<String>()
        var haupt = ""
        d.rawQuery(
            "SELECT g.name, pg.haupt FROM pflanze_gruppe pg JOIN gruppe g ON g.id = pg.gruppe_id " +
                "WHERE pg.pflanze_id = ? ORDER BY pg.haupt DESC, g.id", arg,
        ).use { c ->
            while (c.moveToNext()) {
                gruppen += c.getString(0)
                if (c.getInt(1) == 1) haupt = c.getString(0)
            }
        }
        if (haupt.isEmpty() && gruppen.isNotEmpty()) haupt = gruppen[0]
        val namen = mutableListOf<NameInfo>()
        val quellenSchluessel = linkedSetOf<String>()
        d.rawQuery(
            "SELECT name, art, quelle FROM name WHERE pflanze_id = ? AND art <> 'haupt' ORDER BY id", arg,
        ).use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == "lateinisch" && c.getString(0) == kopf[1]) continue
                namen += NameInfo(c.getString(0), c.getString(1))
                if (!c.isNull(2)) quellenSchluessel += c.getString(2)
            }
        }
        val angaben = mutableListOf<AngabeInfo>()
        d.rawQuery(
            "SELECT abschnitt, text, quelle, auszug FROM angabe WHERE pflanze_id = ? ORDER BY id", arg,
        ).use { c ->
            while (c.moveToNext()) {
                angaben += AngabeInfo(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1)
                quellenSchluessel += c.getString(2)
            }
        }
        val quellen = quellenSchluessel.mapNotNull { quelle(d, it) }
        val bild = d.rawQuery(
            "SELECT datei, urheber, lizenz, lizenz_url, seite, titel, herkunft, abruf FROM bild WHERE pflanze_id = ?", arg,
        ).use { c ->
            if (!c.moveToFirst()) null
            else BildInfo(
                c.getString(0), c.getString(1)?.takeIf { it.isNotBlank() }, c.getString(2),
                c.getString(3)?.takeIf { it.isNotBlank() }, c.getString(4)?.takeIf { it.isNotBlank() },
                c.getString(5)?.takeIf { it.isNotBlank() }, c.getString(6), c.getString(7),
            )
        }
        return PflanzeDetail(id, kopf[0], kopf[1], kopf[2], kopf[3], kopf[4], haupt, gruppen, namen, angaben, quellen, bild)
    }

    private fun quelle(d: SQLiteDatabase, schluessel: String): QuelleInfo? =
        d.rawQuery(
            "SELECT art, titel, adresse, version, lizenz, abrufdatum, aenderung FROM quelle WHERE schluessel = ?",
            arrayOf(schluessel),
        ).use { c ->
            if (!c.moveToFirst()) null
            else QuelleInfo(
                schluessel, c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                c.getString(4), c.getString(5), c.getString(6),
            )
        }

    /** Wie viele Quellen welcher Art und Lizenz in dieser Datenversion stecken (für "Über GartenManager"). */
    fun quellenUebersicht(context: Context): List<QuellenZaehlung> {
        val d = oeffne(context)
        return d.rawQuery(
            "SELECT art, lizenz, COUNT(*) FROM quelle GROUP BY art, lizenz ORDER BY art, lizenz", null,
        ).use { c ->
            val liste = mutableListOf<QuellenZaehlung>()
            while (c.moveToNext()) liste += QuellenZaehlung(c.getString(0), c.getString(1), c.getInt(2))
            liste
        }
    }

    /** Wie viele Pflanzenfotos unter welcher Lizenz stehen (für "Über GartenManager"). */
    fun bilderUebersicht(context: Context): List<Pair<String, Int>> {
        val d = oeffne(context)
        return d.rawQuery("SELECT lizenz, COUNT(*) FROM bild GROUP BY lizenz ORDER BY COUNT(*) DESC, lizenz", null).use { c ->
            val liste = mutableListOf<Pair<String, Int>>()
            while (c.moveToNext()) liste += c.getString(0) to c.getInt(1)
            liste
        }
    }
}
