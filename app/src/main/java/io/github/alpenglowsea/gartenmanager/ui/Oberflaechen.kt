package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import kotlin.math.floor

/**
 * Die Oberflächen, die eine Fläche haben kann. Gespeichert wird nur der [schluessel]
 * (zum Beispiel "gras"), nicht die Farbe. So lässt sich das Aussehen später ändern, ohne
 * gespeicherte Gärten anzufassen. Neue Oberflächen lassen sich hier einfach ergänzen.
 */
enum class Oberflaeche(
    val schluessel: String,
    val nameRes: Int,
    val fuellung: Color,
    val rand: Color,
    val muster: Color,
) {
    GRAS("gras", R.string.ofl_gras, Color(0xFF9CCC65), Color(0xFF558B2F), Color(0xFF689F38)),
    ERDE("erde", R.string.ofl_erde, Color(0xFF8D6E63), Color(0xFF4E342E), Color(0xFF5D4037)),
    KIES("kies", R.string.ofl_kies, Color(0xFFE3E3E0), Color(0xFF8A8A86), Color(0xFFB0B0AB)),
    PFLASTER("pflaster", R.string.ofl_pflaster, Color(0xFFB9A29E), Color(0xFF6D5A57), Color(0xFF7A6562)),
    HOLZ("holz", R.string.ofl_holz, Color(0xFFCFA06A), Color(0xFF7B5127), Color(0xFF8D5E2E)),
    SAND("sand", R.string.ofl_sand, Color(0xFFEAD9A8), Color(0xFFA38B50), Color(0xFFC9B27C)),
    WASSER("wasser", R.string.ofl_wasser, Color(0xFF64B5F6), Color(0xFF1565C0), Color(0xFFE3F2FD)),
    BETON("beton", R.string.ofl_beton, Color(0xFF6F7478), Color(0xFF3F4346), Color(0xFF565B5F)),
    ;

    companion object {
        /** Unbekannte Schlüssel (zum Beispiel aus einer neueren App-Version) werden als Gras gezeigt. */
        fun vonSchluessel(schluessel: String): Oberflaeche =
            values().firstOrNull { it.schluessel == schluessel } ?: GRAS
    }
}

/** Füllung, Rand und Musterfarbe einer Fläche. */
data class Aussehen(val fuellung: Color, val rand: Color, val muster: Color)

/**
 * Das Aussehen einer Oberfläche. Mit gewählter [farbe] bekommt die Fläche diese Füllung; Rand und
 * Muster werden daraus abgeleitet (dunkler, bei Wasser heller), damit das Muster erkennbar bleibt.
 */
fun Oberflaeche.aussehen(farbe: Int?): Aussehen {
    if (farbe == null) return Aussehen(fuellung, rand, muster)
    val f = Color(farbe)
    val m = if (this == Oberflaeche.WASSER) lerp(f, Color.White, 0.8f) else lerp(f, Color.Black, 0.25f)
    return Aussehen(f, lerp(f, Color.Black, 0.4f), m)
}

/** Kleines Farbfeld als Vorschau einer Oberfläche (mit gewählter Farbe, falls vorhanden). */
@Composable
fun Farbfeld(art: Oberflaeche, farbe: Int? = null) {
    val form = RoundedCornerShape(6.dp)
    val aus = art.aussehen(farbe)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.size(28.dp).background(aus.fuellung, form).border(2.dp, aus.rand, form),
    )
}

/** Auswahl der Oberfläche. [aktuell] ist die bisherige (fett gezeigt), bei einer neuen Fläche null. */
@Composable
fun OberflaechenDialog(aktuell: Oberflaeche?, onWahl: (Oberflaeche) -> Unit, onAbbrechen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text(stringResource(R.string.oberflaeche_dialog_titel)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (art in Oberflaeche.values()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWahl(art) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Farbfeld(art)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = stringResource(art.nameRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (art == aktuell) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onAbbrechen) { Text(stringResource(R.string.abbrechen)) }
        },
    )
}

// ---- Muster ----

/** Größe einer Musterzelle in Skizzeneinheiten. */
private const val MUSTER_ZELLE = 24f

/** Wiederholbarer Pseudo-Zufall zwischen 0 und 1 (derselbe Wert für dieselben Zahlen). */
private fun zufall(i: Int, j: Int, k: Int): Float {
    var h = i * 73856093 xor j * 19349663 xor k * 83492791
    h = h xor (h ushr 13)
    h *= 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 65535f
}

/**
 * Zeichnet das dezente Muster einer Oberfläche innerhalb von [pfad]. Das Muster hängt an der
 * Fläche (Bezugspunkt ist ihr erster Punkt), wandert also beim Verschieben mit. Ist es auf dem
 * Bildschirm zu klein oder zu dicht, wird es weggelassen (nur Farbe).
 */
fun DrawScope.zeichneMuster(
    art: Oberflaeche,
    pfad: Path,
    bild: List<Offset>,
    faktor: Float,
    dichte: Float,
    musterFarbe: Color = art.muster,
) {
    val zelle = MUSTER_ZELLE * faktor
    if (zelle < 9f * dichte || bild.isEmpty()) return
    val minX = bild.minOf { it.x } - zelle
    val maxX = bild.maxOf { it.x } + zelle
    val minY = bild.minOf { it.y } - zelle
    val maxY = bild.maxOf { it.y } + zelle
    if ((maxX - minX) / zelle * ((maxY - minY) / zelle) > 3000f) return
    val anker = bild[0]
    val i0 = floor((minX - anker.x) / zelle).toInt()
    val i1 = floor((maxX - anker.x) / zelle).toInt()
    val j0 = floor((minY - anker.y) / zelle).toInt()
    val j1 = floor((maxY - anker.y) / zelle).toInt()
    val farbe = musterFarbe
    val strich = 1.5f * dichte

    clipPath(pfad) {
        when (art) {
            Oberflaeche.GRAS -> for (j in j0..j1) for (i in i0..i1) {
                val x = anker.x + (i + 0.25f + 0.5f * zufall(i, j, 1)) * zelle
                val y = anker.y + (j + 0.6f + 0.3f * zufall(i, j, 2)) * zelle
                drawLine(farbe, Offset(x, y), Offset(x - 0.14f * zelle, y - 0.38f * zelle), strokeWidth = strich, cap = StrokeCap.Round)
                drawLine(farbe, Offset(x, y), Offset(x, y - 0.5f * zelle), strokeWidth = strich, cap = StrokeCap.Round)
                drawLine(farbe, Offset(x, y), Offset(x + 0.14f * zelle, y - 0.38f * zelle), strokeWidth = strich, cap = StrokeCap.Round)
            }
            Oberflaeche.ERDE -> for (j in j0..j1) for (i in i0..i1) {
                for (k in 0..1) {
                    val x = anker.x + (i + zufall(i, j, 3 + k)) * zelle
                    val y = anker.y + (j + zufall(i, j, 5 + k)) * zelle
                    drawCircle(farbe, radius = (1.2f + 1.4f * zufall(i, j, 7 + k)) * dichte, center = Offset(x, y))
                }
            }
            Oberflaeche.KIES -> for (j in j0..j1) for (i in i0..i1) {
                val x = anker.x + (i + 0.15f + 0.7f * zufall(i, j, 11)) * zelle
                val y = anker.y + (j + 0.15f + 0.7f * zufall(i, j, 12)) * zelle
                val r = (0.07f + 0.08f * zufall(i, j, 13)) * zelle
                drawCircle(if (zufall(i, j, 14) > 0.5f) farbe else Color.White.copy(alpha = 0.6f), radius = r, center = Offset(x, y))
            }
            Oberflaeche.PFLASTER -> {
                val h = zelle * 0.6f
                val b = zelle * 1.2f
                val r0 = floor((minY - anker.y) / h).toInt()
                val r1 = floor((maxY - anker.y) / h).toInt()
                for (r in r0..r1) {
                    val y = anker.y + r * h
                    drawLine(farbe, Offset(minX, y), Offset(maxX, y), strokeWidth = strich)
                    val versatz = if (r % 2 == 0) 0f else b / 2f
                    val k0 = floor((minX - anker.x - versatz) / b).toInt()
                    val k1 = floor((maxX - anker.x - versatz) / b).toInt()
                    for (k in k0..k1) {
                        val x = anker.x + versatz + k * b
                        drawLine(farbe, Offset(x, y), Offset(x, y + h), strokeWidth = strich)
                    }
                }
            }
            Oberflaeche.HOLZ -> {
                val h = zelle * 0.5f
                val b = zelle * 3f
                val r0 = floor((minY - anker.y) / h).toInt()
                val r1 = floor((maxY - anker.y) / h).toInt()
                for (r in r0..r1) {
                    val y = anker.y + r * h
                    drawLine(farbe, Offset(minX, y), Offset(maxX, y), strokeWidth = strich)
                    val k0 = floor((minX - anker.x) / b).toInt() - 1
                    val k1 = floor((maxX - anker.x) / b).toInt()
                    for (k in k0..k1) {
                        val x = anker.x + (k + zufall(r, k, 21)) * b
                        drawLine(farbe, Offset(x, y), Offset(x, y + h), strokeWidth = strich)
                    }
                }
            }
            Oberflaeche.SAND -> for (j in j0..j1) for (i in i0..i1) {
                if (zufall(i, j, 31) > 0.35f) {
                    val x = anker.x + (i + zufall(i, j, 32)) * zelle
                    val y = anker.y + (j + zufall(i, j, 33)) * zelle
                    drawCircle(farbe, radius = 1.1f * dichte, center = Offset(x, y))
                }
            }
            Oberflaeche.WASSER -> {
                val w = zelle * 0.9f
                val h = zelle * 0.8f
                val r0 = floor((minY - anker.y) / h).toInt()
                val r1 = floor((maxY - anker.y) / h).toInt()
                for (r in r0..r1) {
                    val y = anker.y + r * h
                    val versatz = if (r % 2 == 0) 0f else w / 2f
                    val k0 = floor((minX - anker.x - versatz) / w).toInt()
                    val k1 = floor((maxX - anker.x - versatz) / w).toInt()
                    for (k in k0..k1) {
                        val x = anker.x + versatz + k * w
                        val welle = Path()
                        welle.moveTo(x, y)
                        welle.cubicTo(x + w * 0.3f, y - w * 0.25f, x + w * 0.7f, y + w * 0.25f, x + w * 0.9f, y)
                        drawPath(welle, farbe, style = Stroke(width = strich, cap = StrokeCap.Round))
                    }
                }
            }
            Oberflaeche.BETON -> for (j in j0..j1) for (i in i0..i1) {
                if (zufall(i, j, 41) > 0.5f) {
                    val x = anker.x + (i + zufall(i, j, 42)) * zelle
                    val y = anker.y + (j + zufall(i, j, 43)) * zelle
                    drawCircle(farbe, radius = 0.9f * dichte, center = Offset(x, y))
                }
            }
        }
        Unit
    }
}
