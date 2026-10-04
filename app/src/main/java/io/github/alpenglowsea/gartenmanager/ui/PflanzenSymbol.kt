package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Eine einzelne Form eines Piktogramms auf einer Fläche von 0 bis 100. */
internal sealed interface SymbolBefehl {
    class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val marke: String) : SymbolBefehl
    class Vieleck(val marke: String, val punkte: FloatArray) : SymbolBefehl
    class Linie(val marke: String, val breite: Float, val punkte: FloatArray) : SymbolBefehl
}

/** Ein Piktogramm mit Gruppenfarbe, fertig gelesen aus [SYMBOL_ROHDATEN]. */
internal class GruppenSymbol(val gruppe: String, val farbe: Color, val befehle: List<SymbolBefehl>)

private fun liesBefehle(text: String): List<SymbolBefehl> {
    val liste = mutableListOf<SymbolBefehl>()
    for (zeile in text.lines()) {
        val t = zeile.trim().split(' ').filter { it.isNotEmpty() }
        if (t.isEmpty()) continue
        when (t[0]) {
            "K" -> liste += SymbolBefehl.Ellipse(t[1].toFloat(), t[2].toFloat(), t[3].toFloat(), t[4].toFloat(), t[5])
            "V" -> liste += SymbolBefehl.Vieleck(t[1], FloatArray(t.size - 2) { t[it + 2].toFloat() })
            "L" -> liste += SymbolBefehl.Linie(t[1], t[2].toFloat(), FloatArray(t.size - 3) { t[it + 3].toFloat() })
        }
    }
    return liste
}

private val SYMBOLE: Map<String, GruppenSymbol> by lazy {
    SYMBOL_ROHDATEN.associate { r ->
        r.gruppe to GruppenSymbol(r.gruppe, Color(0xFF000000.toInt() or r.farbe), liesBefehle(r.befehle))
    }
}

/** Standardfarbe einer Gruppe (grau, falls die Gruppe unbekannt ist). */
fun gruppenFarbe(gruppe: String): Color = SYMBOLE[gruppe]?.farbe ?: Color(0xFF9E9E9E)

internal fun dunkler(c: Color, faktor: Float) = Color(c.red * faktor, c.green * faktor, c.blue * faktor, c.alpha)

private fun markenFarbe(marke: String, akzent: Color): Color = when (marke) {
    "A" -> akzent
    "W85" -> Color(216, 216, 216)
    "W75" -> Color(191, 191, 191)
    "W70" -> Color(178, 178, 178)
    else -> Color.White
}

/** Zeichnet die Formen eines Piktogramms in ein Quadrat mit Ecke [ursprung] und Kantenlänge [kante]. */
internal fun DrawScope.zeichneSymbolFormen(befehle: List<SymbolBefehl>, ursprung: Offset, kante: Float, akzent: Color) {
    val s = kante / 100f
    for (b in befehle) {
        when (b) {
            is SymbolBefehl.Ellipse -> drawOval(
                color = markenFarbe(b.marke, akzent),
                topLeft = Offset(ursprung.x + (b.cx - b.rx) * s, ursprung.y + (b.cy - b.ry) * s),
                size = Size(2f * b.rx * s, 2f * b.ry * s),
            )
            is SymbolBefehl.Vieleck -> {
                val pfad = Path()
                var i = 0
                while (i + 1 < b.punkte.size) {
                    val x = ursprung.x + b.punkte[i] * s
                    val y = ursprung.y + b.punkte[i + 1] * s
                    if (i == 0) pfad.moveTo(x, y) else pfad.lineTo(x, y)
                    i += 2
                }
                pfad.close()
                drawPath(pfad, markenFarbe(b.marke, akzent))
            }
            is SymbolBefehl.Linie -> {
                val pfad = Path()
                var i = 0
                while (i + 1 < b.punkte.size) {
                    val x = ursprung.x + b.punkte[i] * s
                    val y = ursprung.y + b.punkte[i + 1] * s
                    if (i == 0) pfad.moveTo(x, y) else pfad.lineTo(x, y)
                    i += 2
                }
                drawPath(
                    pfad,
                    markenFarbe(b.marke, akzent),
                    style = Stroke(width = b.breite * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/**
 * Zeichnet eine ganze Pflanze: Kreis in [farbe], dunklerer Rand, Piktogramm der [gruppe].
 * Mit [mitInitialen] wird das Piktogramm kleiner und oben gezeichnet, Platz für die Initialen unten.
 * Wird in 4d auch für Pflanzen im Gartenplan verwendet.
 */
internal fun DrawScope.zeichnePflanzenKreis(
    mitte: Offset,
    durchmesser: Float,
    farbe: Color,
    gruppe: String,
    mitInitialen: Boolean,
) {
    val r = durchmesser / 2f
    drawCircle(farbe, radius = r, center = mitte)
    drawCircle(
        dunkler(farbe, 0.6f),
        radius = r - durchmesser * 0.015f,
        center = mitte,
        style = Stroke(width = durchmesser * 0.03f),
    )
    val symbol = SYMBOLE[gruppe] ?: return
    val akzent = dunkler(farbe, 0.55f)
    if (mitInitialen) {
        val kante = durchmesser * 0.54f
        zeichneSymbolFormen(symbol.befehle, Offset(mitte.x - kante / 2f, mitte.y - r + durchmesser * 0.10f), kante, akzent)
    } else {
        val kante = durchmesser * 0.84f
        zeichneSymbolFormen(symbol.befehle, Offset(mitte.x - kante / 2f, mitte.y - kante / 2f), kante, akzent)
    }
}

private val FUELLWOERTER = setOf("und", "der", "die", "das", "von", "vom", "zur", "zum", "im", "am")

/**
 * Initialen des umgangssprachlichen Namens: je ein Buchstabe der ersten beiden Wortteile
 * (Leerzeichen und Bindestrich trennen; Klammern und kleine Füllwörter zählen nicht).
 * Bei einem einzigen Wort die ersten beiden Buchstaben. Beispiele: Wald-Erdbeere → WE, Tomate → To.
 */
fun initialen(name: String): String {
    val ohneKlammer = name.replace(Regex("\\(.*?\\)"), " ")
    val teile = ohneKlammer.split(' ', '-').filter { it.isNotBlank() && it.lowercase() !in FUELLWOERTER }
    return when {
        teile.size >= 2 -> (teile[0].first().uppercaseChar().toString() + teile[1].first().uppercaseChar())
        teile.size == 1 -> teile[0].take(2).let { it.first().uppercaseChar() + it.drop(1).lowercase() }
        else -> ""
    }
}

/** Runde Pflanzenmarke mit Piktogramm, optional mit Initialen. [farbe] ist ein ARGB-Wert aus den Farbfeldern oder null (Gruppenfarbe). */
@Composable
fun PflanzenSymbol(
    gruppe: String,
    name: String,
    groesse: Dp,
    modifier: Modifier = Modifier,
    farbe: Int? = null,
    mitInitialen: Boolean = false,
) {
    val grundfarbe = if (farbe != null) Color(farbe) else gruppenFarbe(gruppe)
    Box(modifier.size(groesse)) {
        Canvas(Modifier.fillMaxSize()) {
            zeichnePflanzenKreis(Offset(size.width / 2f, size.height / 2f), size.minDimension, grundfarbe, gruppe, mitInitialen)
        }
        if (mitInitialen) {
            val schrift = with(LocalDensity.current) { (groesse * 0.2f).toSp() }
            Text(
                text = initialen(name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = schrift,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = groesse * 0.14f),
            )
        }
    }
}
