package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        Grundstueck::class, Garten::class, Flaeche::class, Punkt::class,
        Gegenstand::class, Gegenstandspunkt::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class MeineDatenDb : RoomDatabase() {

    abstract fun gartenDao(): GartenDao

    companion object {
        @Volatile
        private var instanz: MeineDatenDb? = null

        fun holen(context: Context): MeineDatenDb =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeineDatenDb::class.java,
                    "meine_daten.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instanz = it }
            }
    }
}
