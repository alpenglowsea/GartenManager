package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.alpenglowsea.gartenmanager.R
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Längste Notiz zu einer gesetzten Pflanze (Zeichen). */
internal const val MAX_PFLANZ_NOTIZ = 500

/** Längste Notiz zu einem Abschnitt im Info-Fenster (Zeichen). */
internal const val MAX_ABSCHNITT_NOTIZ = 2000

/** In der Info-Karte im Garten wird eine Notiz nach so vielen Zeichen abgeschnitten (und auf höchstens drei Zeilen). */
internal const val KARTE_NOTIZ_ZEICHEN = 120
internal const val KARTE_NOTIZ_ZEILEN = 3

/** Kürzt einen Text auf [max] Zeichen und hängt "…" an, wenn etwas fehlt. Zeilenumbrüche werden zu Leerzeichen. */
internal fun kuerze(text: String, max: Int): String {
    val flach = text.trim().replace(Regex("\\s+"), " ")
    return if (flach.length <= max) flach else flach.take(max).trimEnd() + "…"
}

/**
 * Text für die Info-Karte: Pflanzdatum und, wie alt die Pflanze ist
 * (zum Beispiel "Gepflanzt am 12.05.2023 (2 Jahre, 3 Monate)").
 */
@Composable
internal fun pflanzdatumText(tag: Long): String {
    val res = LocalContext.current.resources
    val datum = LocalDate.ofEpochDay(tag)
    val heute = LocalDate.now()
    val format = datum.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    if (datum.isAfter(heute)) return stringResource(R.string.pflanze_geplant, format)
    val p = Period.between(datum, heute)
    val alter: String? = when {
        p.years > 0 -> {
            val jahre = res.getQuantityString(R.plurals.alter_jahre, p.years, p.years)
            if (p.months > 0) jahre + ", " + res.getQuantityString(R.plurals.alter_monate, p.months, p.months) else jahre
        }
        p.months > 0 -> res.getQuantityString(R.plurals.alter_monate, p.months, p.months)
        p.days >= 7 -> res.getQuantityString(R.plurals.alter_wochen, p.days / 7, p.days / 7)
        p.days > 0 -> res.getQuantityString(R.plurals.alter_tage, p.days, p.days)
        else -> null
    }
    return if (alter == null) {
        stringResource(R.string.pflanze_heute_gepflanzt, format)
    } else {
        stringResource(R.string.pflanze_gepflanzt, format, alter)
    }
}

/** Eingabe einer Notiz über mehrere Zeilen. Ein leerer Text beim Speichern löscht die Notiz. */
@Composable
internal fun NotizDialog(
    titel: String,
    startwert: String,
    maxLaenge: Int,
    onSpeichern: (String) -> Unit,
    onAbbruch: () -> Unit,
) {
    var text by remember { mutableStateOf(startwert) }
    AlertDialog(
        onDismissRequest = onAbbruch,
        title = { Text(titel) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { neu -> if (neu.length <= maxLaenge) text = neu },
                minLines = 3,
                maxLines = 8,
                supportingText = { Text("${text.length} / $maxLaenge") },
            )
        },
        confirmButton = {
            Row {
                if (startwert.isNotBlank()) {
                    TextButton(onClick = { onSpeichern("") }) { Text(stringResource(R.string.loeschen)) }
                }
                TextButton(onClick = { onSpeichern(text.trim()) }) { Text(stringResource(R.string.speichern)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

/** Auswahl des Pflanzdatums. [startTag] ist der Tag seit 01.01.1970 oder null. Speichern mit null entfernt das Datum. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PflanzdatumDialog(startTag: Long?, onSpeichern: (Long?) -> Unit, onAbbruch: () -> Unit) {
    val tagMillis = 86_400_000L
    val zustand = rememberDatePickerState(initialSelectedDateMillis = (startTag ?: LocalDate.now().toEpochDay()) * tagMillis)
    DatePickerDialog(
        onDismissRequest = onAbbruch,
        confirmButton = {
            TextButton(
                onClick = { onSpeichern(zustand.selectedDateMillis?.let { Math.floorDiv(it, tagMillis) }) },
                enabled = zustand.selectedDateMillis != null,
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (startTag != null) {
                    TextButton(onClick = { onSpeichern(null) }) { Text(stringResource(R.string.pflanzdatum_entfernen)) }
                }
                TextButton(onClick = onAbbruch) { Text(stringResource(R.string.abbrechen)) }
            }
        },
    ) {
        DatePicker(state = zustand)
    }
}
