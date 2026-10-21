package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context

/** Die Textabschnitte, zu denen es Angaben geben kann (ohne "Bezeichnung" und "Gattung", die aus den Namen entstehen). */
val TEXT_ABSCHNITTE = listOf("Herkunft", "Standort", "Wuchs und Laub", "Blüte", "Verwendung", "Pflege")

/**
 * Sucht in den Pflanzen der Grunddaten und in den eigenen Pflanzen; beide stehen gemeinsam in alphabetischer
 * Reihenfolge. Mit [nurEigene] zeigt die Liste nur die eigenen Pflanzen.
 * [gruppen] ordnet Gruppennamen den Nummern zu, mit denen der Gruppenfilter arbeitet.
 */
suspend fun sucheMitEigenen(
    context: Context,
    text: String,
    gruppenIds: Set<Long>,
    gruppen: List<GruppeMitAnzahl>,
    nurEigene: Boolean = false,
): List<PflanzeTreffer> {
    val ausGrunddaten = if (nurEigene) emptyList() else Grunddaten.suche(context, text, gruppenIds)
    val dao = MeineDatenDb.holen(context).gartenDao()
    val q = Grunddaten.normalisiere(text)
    val eigene = dao.eigenePflanzenListe().filter { p ->
        val gruppeOk = gruppenIds.isEmpty() || gruppen.any { it.name == p.gruppe && it.id in gruppenIds }
        val textOk = q.isEmpty() || listOf(p.name, p.lateinisch, p.gattung).any { Grunddaten.normalisiere(it).contains(q) }
        gruppeOk && textOk
    }.map { p ->
        PflanzeTreffer(
            id = EIGENE_BASIS + p.id,
            hauptname = p.name,
            lateinisch = p.lateinisch,
            hauptgruppe = p.gruppe,
            gefundenUeber = null,
            bild = null,
            eigene = true,
        )
    }
    if (eigene.isEmpty()) return ausGrunddaten
    // Gemeinsam alphabetisch (dieselbe Normalisierung wie bei der Suche); die Reihenfolge innerhalb gleicher Namen bleibt.
    return (ausGrunddaten + eigene).sortedBy { Grunddaten.normalisiere(it.hauptname) }
}

/** Baut aus einer eigenen Pflanze und ihren Texten dasselbe Datenpaket, das das Info-Fenster für Katalogpflanzen nutzt. */
fun baueEigenesDetail(pflanze: EigenePflanze, angaben: List<EigeneAngabe>): PflanzeDetail =
    PflanzeDetail(
        id = EIGENE_BASIS + pflanze.id,
        hauptname = pflanze.name,
        lateinisch = pflanze.lateinisch,
        gattung = pflanze.gattung,
        wikipedia = null,
        wikidata = null,
        hauptgruppe = pflanze.gruppe,
        gruppen = listOf(pflanze.gruppe),
        namen = emptyList(),
        angaben = TEXT_ABSCHNITTE.mapNotNull { abschnitt ->
            angaben.firstOrNull { it.abschnitt == abschnitt }?.let { AngabeInfo(abschnitt, it.text, QUELLE_EIGEN, false) }
        },
        quellen = emptyList(),
        bild = null,
    )

/** Kennzeichnung für Angaben, die der Nutzer selbst geschrieben hat. */
const val QUELLE_EIGEN = "eigen"
