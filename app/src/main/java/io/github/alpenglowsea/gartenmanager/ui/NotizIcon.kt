package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R

private fun symbol(name: String, pfad: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pfad).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()

// Formen der Material-Symbole "edit" und "place" (Apache-Lizenz 2.0), im Code gezeichnet
private val StiftBild: ImageVector by lazy {
    symbol(
        "stift",
        "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04a1,1 0,0 0,0 -1.41l-2.34,-2.34a1,1 0,0 0,-1.41 0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
    )
}
private val NadelBild: ImageVector by lazy {
    symbol(
        "nadel",
        "M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5A2.5,2.5 0,1 1,12 6.5a2.5,2.5 0,0 1,0 5z",
    )
}

private val NotizGelb = Color(0xFFF3C433)
private val NotizDunkel = Color(0xFF3A2F00)

/**
 * Kleiner gelber Punkt mit Stift: Zu dieser Pflanze gehört eine Notiz. [rand] ist die Farbe des Untergrunds,
 * damit der Punkt am Bild mit einem sauberen Rand abschließt.
 */
@Composable
internal fun NotizAbzeichen(rand: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(20.dp)
            .background(NotizGelb, CircleShape)
            .border(BorderStroke(2.dp, rand), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = StiftBild,
            contentDescription = stringResource(R.string.mp_hat_notiz),
            tint = NotizDunkel,
            modifier = Modifier.size(11.dp),
        )
    }
}

/** Standortnadel: springt in den Gartenplan und zeigt dort die Pflanze(n). */
@Composable
internal fun StandortKnopf(beschreibung: String, onClick: () -> Unit, modifier: Modifier = Modifier, groesse: Dp = 30.dp) {
    IconButton(onClick = onClick, modifier = modifier) {
        Box(
            Modifier.size(groesse).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = NadelBild,
                contentDescription = beschreibung,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
