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
)

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
                "WHERE pg.pflanze_id = p.id AND pg.haupt = 1), $gefunden " +
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
                liste += PflanzeTreffer(c.getLong(0), name, lat, c.getString(3) ?: "", ueber)
            }
            liste
        }
    }
}
