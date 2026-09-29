package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.Garten

/**
 * Vorläufiger Bildschirm für einen geöffneten Garten. Die Zeichenfläche selbst
 * kommt mit Teilschritt 2b. Hier zeigt sich schon der Unterschied zwischen
 * Ansicht (Standard) und Bearbeiten (farblich gekennzeichnet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GartenPlatzhalterScreen(
    garten: Garten,
    bearbeiten: Boolean,
    onBearbeiten: () -> Unit,
    onFertig: () -> Unit,
    onZurueck: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (bearbeiten) {
                            stringResource(R.string.bearbeiten_titel, garten.name)
                        } else {
                            garten.name
                        },
                    )
                },
                navigationIcon = {
                    TextButton(onClick = if (bearbeiten) onFertig else onZurueck) {
                        Text(stringResource(R.string.zurueck))
                    }
                },
                actions = {
                    if (bearbeiten) {
                        TextButton(onClick = onFertig) { Text(stringResource(R.string.fertig)) }
                    } else {
                        TextButton(onClick = onBearbeiten) { Text(stringResource(R.string.bearbeiten)) }
                    }
                },
                colors = if (bearbeiten) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(
                    if (bearbeiten) R.string.platzhalter_bearbeiten else R.string.platzhalter_ansicht,
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
