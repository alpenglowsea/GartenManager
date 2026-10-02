package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R

/**
 * Die 12 festen Farbfelder für Gegenstände und Flächen (ARGB als Zahl, so wie sie in der
 * Datenbank stehen). "Standard" (leer) ist keine eigene Farbe, sondern die Standardfarbe des
 * Gegenstands oder der Oberfläche.
 */
val FARBFELDER: List<Int> = listOf(
    0xFFC0392B.toInt(), // Rot
    0xFFE67E22.toInt(), // Orange
    0xFFF1C40F.toInt(), // Gelb
    0xFF8BC34A.toInt(), // Hellgrün
    0xFF3E8E41.toInt(), // Dunkelgrün
    0xFF26A69A.toInt(), // Türkis
    0xFF64B5F6.toInt(), // Hellblau
    0xFF2F5FA8.toInt(), // Dunkelblau
    0xFF8E5AA8.toInt(), // Violett
    0xFFE58AB0.toInt(), // Rosa
    0xFF8D6E63.toInt(), // Braun
    0xFF9E9E9E.toInt(), // Grau
)

/** Auswahl einer der 12 Farben oder "Standard" (null). [aktuell] wird mit einem dicken Rand gezeigt. */
@Composable
fun FarbDialog(aktuell: Int?, onWahl: (Int?) -> Unit, onAbbrechen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text(stringResource(R.string.farbe_dialog_titel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (zeile in FARBFELDER.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (farbe in zeile) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(farbe), CircleShape)
                                    .border(
                                        if (farbe == aktuell) 4.dp else 1.dp,
                                        if (farbe == aktuell) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        CircleShape,
                                    )
                                    .clickable { onWahl(farbe) },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { onWahl(null) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.farbe_standard),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onAbbrechen) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}
