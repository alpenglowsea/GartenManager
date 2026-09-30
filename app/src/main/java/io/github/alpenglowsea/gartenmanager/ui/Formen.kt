package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.ui.geometry.Offset
import io.github.alpenglowsea.gartenmanager.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vorgegebene geometrische Formen zum Aufziehen. [quadratisch] heißt: Die Form wird
 * immer in ein Quadrat gezogen (Quadrat, Kreis, regelmäßige Vielecke).
 */
enum class Form(val nameRes: Int, val quadratisch: Boolean) {
    RECHTECK(R.string.form_rechteck, false),
    QUADRAT(R.string.form_quadrat, true),
    KREIS(R.string.form_kreis, true),
    ELLIPSE(R.string.form_ellipse, false),
    DREIECK(R.string.form_dreieck, false),
    FUENFECK(R.string.form_fuenfeck, true),
    SECHSECK(R.string.form_sechseck, true),
    ACHTECK(R.string.form_achteck, true),
}

/** Ein Punkt einer Form: Lage in Skizzenkoordinaten und ob die Kurve dort rund läuft. */
data class FormPunkt(val lage: Offset, val rund: Boolean)

/**
 * Berechnet die Punkte einer Form, die von [a] nach [b] (gegenüberliegende Ecken des
 * umschließenden Rechtecks) aufgezogen wird. Ist die Form zu klein ([minimum] in
 * Skizzeneinheiten), kommt eine leere Liste zurück.
 *
 * Kreis und Ellipse bestehen aus 8 runden Punkten; dadurch lassen sie sich später
 * wie jede andere Fläche durch Ziehen der Punkte verändern.
 */
fun formPunkte(form: Form, a: Offset, b: Offset, minimum: Float): List<FormPunkt> {
    var dx = b.x - a.x
    var dy = b.y - a.y
    if (form.quadratisch) {
        val seite = maxOf(abs(dx), abs(dy))
        dx = if (dx < 0f) -seite else seite
        dy = if (dy < 0f) -seite else seite
    }
    if (abs(dx) < minimum || abs(dy) < minimum) return emptyList()

    val x0 = minOf(a.x, a.x + dx)
    val x1 = maxOf(a.x, a.x + dx)
    val y0 = minOf(a.y, a.y + dy)
    val y1 = maxOf(a.y, a.y + dy)
    val mx = (x0 + x1) / 2f
    val my = (y0 + y1) / 2f
    val rx = (x1 - x0) / 2f
    val ry = (y1 - y0) / 2f

    return when (form) {
        Form.RECHTECK, Form.QUADRAT -> listOf(
            FormPunkt(Offset(x0, y0), false),
            FormPunkt(Offset(x1, y0), false),
            FormPunkt(Offset(x1, y1), false),
            FormPunkt(Offset(x0, y1), false),
        )
        Form.DREIECK -> listOf(
            FormPunkt(Offset(mx, y0), false),
            FormPunkt(Offset(x1, y1), false),
            FormPunkt(Offset(x0, y1), false),
        )
        Form.KREIS, Form.ELLIPSE -> punkteAufEllipse(8, mx, my, rx, ry, rund = true)
        Form.FUENFECK -> punkteAufEllipse(5, mx, my, rx, ry, rund = false)
        Form.SECHSECK -> punkteAufEllipse(6, mx, my, rx, ry, rund = false)
        Form.ACHTECK -> punkteAufEllipse(8, mx, my, rx, ry, rund = false)
    }
}

/** n Punkte gleichmäßig auf einer Ellipse, der erste ganz oben. */
private fun punkteAufEllipse(n: Int, mx: Float, my: Float, rx: Float, ry: Float, rund: Boolean): List<FormPunkt> {
    val pi = Math.PI.toFloat()
    return List(n) { i ->
        val winkel = -pi / 2f + 2f * pi * i / n
        FormPunkt(Offset(mx + rx * cos(winkel), my + ry * sin(winkel)), rund)
    }
}
