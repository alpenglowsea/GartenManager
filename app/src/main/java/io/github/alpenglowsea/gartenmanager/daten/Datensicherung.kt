package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Sicherung und Wiederherstellung von "Meine Daten": Gärten, Pflanzen im Plan, "Meine Pflanzen", eigene Pflanzen,
 * Notizen und eigene Fotos. Die Sicherung ist eine ZIP-Datei mit der Datenbank (meine_daten.db), den eigenen Fotos
 * (eigene_bilder/) und einer kleinen Textdatei mit Angaben (info.txt). Die Grunddaten (Pflanzenkatalog) gehören
 * nicht dazu, sie kommen mit der App.
 */
object Datensicherung {
    private const val INFO = "info.txt"
    private const val BILDER = "eigene_bilder"
    private val BILD_NAME = Regex("^[0-9]+\\.jpg$")

    /** Ältere Sicherungen als diese Version lassen sich nicht mehr einspielen (dafür gibt es keine Umbauschritte). */
    const val AELTESTE_VERSION = 5

    enum class Fehler { KEINE_SICHERUNG, ZU_NEU, ZU_ALT, BESCHAEDIGT, LESEFEHLER }

    /** Ergebnis der Prüfung einer Sicherungsdatei; [ordner] enthält bei Erfolg die entpackten Teile. */
    class Pruefung(
        val ordner: File?,
        val fehler: Fehler?,
        val erstellt: String = "",
        val gaerten: Int = 0,
        val fotos: Int = 0,
    )

    fun vorschlagName(): String =
        "gartenmanager-sicherung-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.GERMANY).format(Date()) + ".zip"

    private fun datenbankDatei(context: Context): File = context.applicationContext.getDatabasePath(MeineDatenDb.DATEI)

    private fun zaehle(db: androidx.sqlite.db.SupportSQLiteDatabase, tabelle: String): Int =
        db.query("SELECT COUNT(*) FROM $tabelle").use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * Schreibt die Sicherung in [ziel]. Gibt Anzahl Gärten und Fotos zurück oder null bei einem Fehler.
     * Muss in einem Hintergrund-Thread laufen.
     */
    fun exportiere(context: Context, ziel: Uri): Pair<Int, Int>? {
        return try {
            val app = context.applicationContext
            val db = MeineDatenDb.holen(app).openHelper.writableDatabase
            // Alles aus dem Zwischenspeicher der Datenbank in die Hauptdatei schreiben, damit die Kopie vollständig ist
            db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            val gaerten = zaehle(db, "garten")
            val fotos = EigeneBilder.ordner(app).listFiles()?.filter { BILD_NAME.matches(it.name) && it.length() > 0 }.orEmpty()
            val version = try {
                app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
            } catch (e: Exception) {
                "?"
            }
            val info = "GartenManager-Sicherung\n" +
                "appVersion=$version\n" +
                "datenbankVersion=${MeineDatenDb.VERSION}\n" +
                "erstellt=" + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date()) + "\n"
            val ausgang = app.contentResolver.openOutputStream(ziel, "w") ?: return null
            ausgang.use { raus ->
                ZipOutputStream(BufferedOutputStream(raus)).use { zip ->
                    zip.putNextEntry(ZipEntry(INFO))
                    zip.write(info.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry(MeineDatenDb.DATEI))
                    datenbankDatei(app).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    for (f in fotos) {
                        zip.putNextEntry(ZipEntry("$BILDER/${f.name}"))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            gaerten to fotos.size
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    private fun arbeitsOrdner(context: Context) = File(context.applicationContext.cacheDir, "einspielen")

    /** Räumt die zwischengespeicherte Sicherung weg (nach Abbruch oder Erfolg). */
    fun raeumeAuf(context: Context) {
        arbeitsOrdner(context).deleteRecursively()
    }

    /**
     * Liest die Sicherung in einen Zwischenordner und prüft, ob sie zu dieser App passt. Es wird noch nichts
     * verändert. Muss in einem Hintergrund-Thread laufen.
     */
    fun pruefe(context: Context, quelle: Uri): Pruefung {
        val app = context.applicationContext
        val arbeit = arbeitsOrdner(app)
        arbeit.deleteRecursively()
        File(arbeit, "bilder").mkdirs()
        fun fehler(f: Fehler): Pruefung {
            arbeit.deleteRecursively()
            return Pruefung(null, f)
        }
        return try {
            var hatDb = false
            var info = ""
            var fotos = 0
            val eingang = app.contentResolver.openInputStream(quelle) ?: return fehler(Fehler.LESEFEHLER)
            eingang.use { rein ->
                ZipInputStream(BufferedInputStream(rein)).use { zip ->
                    while (true) {
                        val eintrag = zip.nextEntry ?: break
                        val name = eintrag.name
                        // Es werden nur genau diese Dateien übernommen, alles andere wird ignoriert
                        if (!eintrag.isDirectory) {
                            when {
                                name == MeineDatenDb.DATEI -> {
                                    File(arbeit, MeineDatenDb.DATEI).outputStream().use { zip.copyTo(it) }
                                    hatDb = true
                                }
                                name == INFO -> info = zip.readBytes().toString(Charsets.UTF_8)
                                name.startsWith("$BILDER/") && BILD_NAME.matches(name.removePrefix("$BILDER/")) -> {
                                    File(arbeit, "bilder/" + name.removePrefix("$BILDER/")).outputStream().use { zip.copyTo(it) }
                                    fotos++
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            val dbDatei = File(arbeit, MeineDatenDb.DATEI)
            if (!hatDb || !dbDatei.exists()) return fehler(Fehler.KEINE_SICHERUNG)
            // Kopfzeile einer SQLite-Datei: "SQLite format 3", ab Byte 60 steht die Version des Aufbaus (von Room gesetzt)
            val kopf = ByteArray(100)
            RandomAccessFile(dbDatei, "r").use { if (it.read(kopf) < 100) return fehler(Fehler.BESCHAEDIGT) }
            if (String(kopf, 0, 15, Charsets.US_ASCII) != "SQLite format 3") return fehler(Fehler.BESCHAEDIGT)
            val version = ByteBuffer.wrap(kopf, 60, 4).int
            if (version > MeineDatenDb.VERSION) return fehler(Fehler.ZU_NEU)
            if (version < AELTESTE_VERSION) return fehler(Fehler.ZU_ALT)
            var gaerten = 0
            val db = SQLiteDatabase.openDatabase(dbDatei.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val ok = db.rawQuery("PRAGMA quick_check", null).use { it.moveToFirst() && it.getString(0) == "ok" }
                val hatTabelle = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='garten'", null).use { it.moveToFirst() }
                if (!ok || !hatTabelle) return fehler(Fehler.BESCHAEDIGT)
                gaerten = db.rawQuery("SELECT COUNT(*) FROM garten", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } finally {
                db.close()
            }
            val erstellt = info.lineSequence().firstOrNull { it.startsWith("erstellt=") }?.removePrefix("erstellt=")?.trim().orEmpty()
            Pruefung(arbeit, null, erstellt, gaerten, fotos)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fehler(Fehler.LESEFEHLER)
        }
    }

    private fun loescheDatenbankDateien(app: Context) {
        val d = datenbankDatei(app)
        for (endung in listOf("", "-wal", "-shm", "-journal")) File(d.path + endung).delete()
    }

    /**
     * Ersetzt "Meine Daten" durch den Inhalt einer geprüften Sicherung. Danach muss die App neu starten.
     * Die neuen Dateien werden zuerst neben die alten gelegt, erst dann werden die alten ersetzt.
     * Gibt false zurück, wenn etwas schiefging. Muss in einem Hintergrund-Thread laufen.
     */
    fun spieleEin(context: Context, pruefung: Pruefung): Boolean {
        val quelle = pruefung.ordner ?: return false
        return try {
            val app = context.applicationContext
            val dbZiel = datenbankDatei(app)
            dbZiel.parentFile?.mkdirs()
            val dbNeu = File(dbZiel.path + ".neu")
            File(quelle, MeineDatenDb.DATEI).copyTo(dbNeu, overwrite = true)
            val bilderNeu = File(app.filesDir, "$BILDER.neu")
            bilderNeu.deleteRecursively()
            bilderNeu.mkdirs()
            File(quelle, "bilder").listFiles()?.forEach { it.copyTo(File(bilderNeu, it.name), overwrite = true) }
            MeineDatenDb.schliessen()
            loescheDatenbankDateien(app)
            if (!dbNeu.renameTo(dbZiel)) return false
            val bilderAlt = File(app.filesDir, BILDER)
            bilderAlt.deleteRecursively()
            if (!bilderNeu.renameTo(bilderAlt)) return false
            raeumeAuf(app)
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    /** Löscht alles, was der Nutzer angelegt hat. Danach muss die App neu starten. Muss in einem Hintergrund-Thread laufen. */
    fun loescheAlles(context: Context): Boolean {
        return try {
            val app = context.applicationContext
            MeineDatenDb.schliessen()
            loescheDatenbankDateien(app)
            File(app.filesDir, BILDER).deleteRecursively()
            app.getSharedPreferences("pflanzen_merker", Context.MODE_PRIVATE).edit().clear().apply()
            raeumeAuf(app)
            !datenbankDatei(app).exists()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    /** Startet die App neu, damit sie die ersetzten Dateien frisch öffnet. */
    fun starteNeu(context: Context) {
        val app = context.applicationContext
        val start = app.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        app.startActivity(start)
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
