package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Datei 2 "Meine Daten": alles, was der Nutzer selbst anlegt. Wird bei App-Updates
 * nie durch neue Grunddaten ersetzt (siehe DATENMODELL.md).
 *
 * ACHTUNG, nur für die Entwicklungsphase: Ändert sich der Aufbau der Datenbank,
 * werden die Testdaten auf dem Handy gelöscht (fallbackToDestructiveMigration).
 * Vor der ersten echten Veröffentlichung muss das durch richtige Umbauschritte
 * (Migrationen) ersetzt werden.
 */
@Database(
    entities = [
        Grundstueck::class, Garten::class, Ebene::class, Flaeche::class, Punkt::class,
        Gegenstand::class, Gegenstandspunkt::class, BildAusschnitt::class,
        EigenePflanze::class, EigeneAngabe::class, MerkerZeile::class, Bestand::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class MeineDatenDb : RoomDatabase() {

    abstract fun gartenDao(): GartenDao

    companion object {
        /** Version 5 -> 6: neue Tabelle für persönliche Bildausschnitte. Bestehende Gärten bleiben erhalten. */
        private val VON_5_AUF_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bild_ausschnitt` (`pflanzeId` INTEGER NOT NULL, " +
                        "`mitteX` REAL NOT NULL, `mitteY` REAL NOT NULL, `zoom` REAL NOT NULL, PRIMARY KEY(`pflanzeId`))",
                )
            }
        }

        /**
         * Version 6 -> 7: gesetzte Pflanzen. Sie sind Gegenstände mit vier zusätzlichen Spalten
         * (Nummer in den Grunddaten und eine Notfall-Kopie von Name, lateinischem Namen und Gruppe).
         * Bestehende Gärten bleiben erhalten.
         */
        private val VON_6_AUF_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `gegenstand` ADD COLUMN `pflanzeId` INTEGER")
                db.execSQL("ALTER TABLE `gegenstand` ADD COLUMN `pflanzeName` TEXT")
                db.execSQL("ALTER TABLE `gegenstand` ADD COLUMN `pflanzeLatein` TEXT")
                db.execSQL("ALTER TABLE `gegenstand` ADD COLUMN `pflanzeGruppe` TEXT")
            }
        }

        /**
         * Version 7 -> 8: Pflanzdatum und Notiz an gesetzten Pflanzen, eigene Pflanzen, eigene Texte zu
         * Abschnitten und der Merker für zugeklappte Abschnitte (bisher in den App-Einstellungen).
         */
        private val VON_7_AUF_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `gegenstand` ADD COLUMN `pflanzdatum` INTEGER")
                db.execSQL("ALTER TABLE `gegenstand` ADD COLUMN `notiz` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `eigene_pflanze` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `lateinisch` TEXT NOT NULL, `gattung` TEXT NOT NULL, " +
                        "`gruppe` TEXT NOT NULL, `angelegtAm` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `eigene_angabe` (`pflanzeId` INTEGER NOT NULL, `abschnitt` TEXT NOT NULL, " +
                        "`text` TEXT NOT NULL, `geaendertAm` INTEGER NOT NULL, PRIMARY KEY(`pflanzeId`, `abschnitt`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `merker` (`pflanzeId` INTEGER NOT NULL, `abschnitt` TEXT NOT NULL, " +
                        "PRIMARY KEY(`pflanzeId`, `abschnitt`))",
                )
            }
        }

        /** Version 8 -> 9: Einträge in "Meine Pflanzen", die nicht im Gartenplan stehen. */
        private val VON_8_AUF_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bestand` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pflanzeId` INTEGER NOT NULL, `pflanzeName` TEXT NOT NULL, `pflanzeLatein` TEXT NOT NULL, " +
                        "`pflanzeGruppe` TEXT NOT NULL, `anzahl` INTEGER NOT NULL, `gartenId` INTEGER, " +
                        "`pflanzdatum` INTEGER, `notiz` TEXT, `angelegtAm` INTEGER NOT NULL)",
                )
            }
        }

        /** Version 9 -> 10: Reihenfolge der Gärten. Alle bisherigen Gärten starten bei 0 und bleiben damit nach Namen sortiert. */
        private val VON_9_AUF_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `garten` ADD COLUMN `reihenfolge` INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instanz: MeineDatenDb? = null

        fun holen(context: Context): MeineDatenDb =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeineDatenDb::class.java,
                    "meine_daten.db",
                )
                    .addMigrations(VON_5_AUF_6, VON_6_AUF_7, VON_7_AUF_8, VON_8_AUF_9, VON_9_AUF_10)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instanz = it }
            }
    }
}
