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
    ],
    version = 7,
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

        @Volatile
        private var instanz: MeineDatenDb? = null

        fun holen(context: Context): MeineDatenDb =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeineDatenDb::class.java,
                    "meine_daten.db",
                )
                    .addMigrations(VON_5_AUF_6, VON_6_AUF_7)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instanz = it }
            }
    }
}
