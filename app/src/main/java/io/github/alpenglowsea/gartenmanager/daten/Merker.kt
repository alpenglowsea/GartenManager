package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context

/**
 * Der Merker für zugeklappte Abschnitte im Info-Fenster liegt seit Teilschritt 4d2 in der Datenbank
 * (Tabelle "merker", Teil von "Meine Daten" und damit des späteren Backups). Diese Funktion übernimmt
 * einmalig die Einträge, die frühere Versionen in den App-Einstellungen abgelegt haben, und löscht sie dort.
 */
suspend fun uebernehmeAlteMerker(context: Context, dao: GartenDao) {
    val prefs = context.applicationContext.getSharedPreferences("pflanzen_merker", Context.MODE_PRIVATE)
    val alle = prefs.all
    if (alle.isEmpty()) return
    for ((schluessel, wert) in alle) {
        val nummer = schluessel.removePrefix("zu_").toLongOrNull() ?: continue
        val abschnitte = (wert as? Set<*>)?.filterIsInstance<String>() ?: continue
        for (abschnitt in abschnitte) dao.setzeMerker(MerkerZeile(nummer, abschnitt))
    }
    prefs.edit().clear().apply()
}
