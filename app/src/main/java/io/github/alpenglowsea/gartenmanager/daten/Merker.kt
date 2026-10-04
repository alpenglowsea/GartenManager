package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context

/**
 * Merkt sich je Pflanzenart, welche Abschnitte im Info-Fenster zugeklappt sind.
 * Vorläufig in den App-Einstellungen; zieht mit 4d in die Datenbank um (Teil des Backups).
 */
class Merker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pflanzen_merker", Context.MODE_PRIVATE)

    fun zugeklappt(pflanzeId: Long): Set<String> =
        prefs.getStringSet("zu_$pflanzeId", emptySet())?.toSet() ?: emptySet()

    fun setze(pflanzeId: Long, abschnitt: String, zugeklappt: Boolean) {
        val neu = zugeklappt(pflanzeId).toMutableSet()
        if (zugeklappt) neu += abschnitt else neu -= abschnitt
        prefs.edit().putStringSet("zu_$pflanzeId", neu).apply()
    }
}
