package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import io.github.alpenglowsea.gartenmanager.R
import kotlin.math.cos
import kotlin.math.sin

/** Gruppen im Katalog der Gegenstände. */
enum class GegenstandsGruppe(val nameRes: Int) {
    GEBAEUDE(R.string.gruppe_gebaeude),
    VERKEHR(R.string.gruppe_verkehr),
    SITZEN(R.string.gruppe_sitzen),
    SPIEL(R.string.gruppe_spiel),
    GARTEN(R.string.gruppe_garten),
    DEKO(R.string.gruppe_deko),
    ZAUN(R.string.gruppe_zaun),
}

/**
 * Katalog der Gegenstände (Teilschritt 3b). [schluessel] steht in der Datenbank, [breiteM] und
 * [hoeheM] sind die Standardgröße in echten Metern (Draufsicht, Vorderseite oben), [farbe] ist
 * die Standardfarbe der Hauptfläche. Zäune, Hecken, Mauern, Bewässerung und das Gebäude in
 * freier Form kommen später.
 */
enum class GegenstandsArt(
    val schluessel: String,
    val nameRes: Int,
    val gruppe: GegenstandsGruppe,
    val breiteM: Float,
    val hoeheM: Float,
    farbeArgb: Long,
) {
    HAUS("haus", R.string.gg_haus, GegenstandsGruppe.GEBAEUDE, 10f, 8f, 0xFFC0603F),
    // Freies Gebäude: Form aus Punkten (Tabelle Gegenstandspunkt); die Maße hier gelten nur für das Rechteck zu Beginn.
    GEBAEUDE_FREI("gebaeude_frei", R.string.gg_gebaeude_frei, GegenstandsGruppe.GEBAEUDE, 8f, 6f, 0xFFC0603F),
    GARAGE("garage", R.string.gg_garage, GegenstandsGruppe.GEBAEUDE, 3f, 6f, 0xFF9EA3A8),
    CARPORT("carport", R.string.gg_carport, GegenstandsGruppe.GEBAEUDE, 3f, 5.5f, 0xFFB9A58A),
    SCHUPPEN("schuppen", R.string.gg_schuppen, GegenstandsGruppe.GEBAEUDE, 2.5f, 2f, 0xFF9C7A54),
    GEWAECHSHAUS("gewaechshaus", R.string.gg_gewaechshaus, GegenstandsGruppe.GEBAEUDE, 3f, 2.5f, 0xFFA8D8D0),
    WAERMEPUMPE("waermepumpe", R.string.gg_waermepumpe, GegenstandsGruppe.GEBAEUDE, 1f, 0.5f, 0xFFD0D3D6),
    LADESAEULE("ladesaeule", R.string.gg_ladesaeule, GegenstandsGruppe.GEBAEUDE, 0.4f, 0.3f, 0xFF4DA67A),

    AUTO("auto", R.string.gg_auto, GegenstandsGruppe.VERKEHR, 1.8f, 4.5f, 0xFF3F72B8),
    MOTORRAD("motorrad", R.string.gg_motorrad, GegenstandsGruppe.VERKEHR, 0.8f, 2.1f, 0xFFB03A3A),
    FAHRRAD("fahrrad", R.string.gg_fahrrad, GegenstandsGruppe.VERKEHR, 0.6f, 1.8f, 0xFF3A8F8F),

    TISCH_RUND("tisch_rund", R.string.gg_tisch_rund, GegenstandsGruppe.SITZEN, 1.2f, 1.2f, 0xFFB08A5E),
    TISCH_ECKIG("tisch_eckig", R.string.gg_tisch_eckig, GegenstandsGruppe.SITZEN, 1.6f, 0.9f, 0xFFB08A5E),
    TISCH_STUEHLE("tisch_stuehle", R.string.gg_tisch_stuehle, GegenstandsGruppe.SITZEN, 2.4f, 2.4f, 0xFFB08A5E),
    STUHL("stuhl", R.string.gg_stuhl, GegenstandsGruppe.SITZEN, 0.5f, 0.5f, 0xFF8A6A4A),
    BANK("bank", R.string.gg_bank, GegenstandsGruppe.SITZEN, 1.5f, 0.5f, 0xFF9A7650),
    LIEGE("liege", R.string.gg_liege, GegenstandsGruppe.SITZEN, 0.7f, 2f, 0xFFD9C27A),
    SONNENSCHIRM("sonnenschirm", R.string.gg_sonnenschirm, GegenstandsGruppe.SITZEN, 2.5f, 2.5f, 0xFFE0B040),
    GRILL("grill", R.string.gg_grill, GegenstandsGruppe.SITZEN, 0.6f, 0.6f, 0xFF4A4A4A),
    FEUERSTELLE("feuerstelle", R.string.gg_feuerstelle, GegenstandsGruppe.SITZEN, 0.9f, 0.9f, 0xFF8C8C88),

    TRAMPOLIN("trampolin", R.string.gg_trampolin, GegenstandsGruppe.SPIEL, 3f, 3f, 0xFF2F3E5C),
    SANDKASTEN("sandkasten", R.string.gg_sandkasten, GegenstandsGruppe.SPIEL, 1.5f, 1.5f, 0xFFE6D08E),
    SCHAUKEL("schaukel", R.string.gg_schaukel, GegenstandsGruppe.SPIEL, 3f, 2f, 0xFFB5834F),
    SPIELHAUS("spielhaus", R.string.gg_spielhaus, GegenstandsGruppe.SPIEL, 1.5f, 1.5f, 0xFFD8793F),
    POOL("pool", R.string.gg_pool, GegenstandsGruppe.SPIEL, 4f, 3f, 0xFF5BB5E0),
    BRUNNEN("brunnen", R.string.gg_brunnen, GegenstandsGruppe.SPIEL, 1f, 1f, 0xFF9A9A96),

    HOCHBEET("hochbeet", R.string.gg_hochbeet, GegenstandsGruppe.GARTEN, 2f, 1f, 0xFFA07850),
    KOMPOSTER("komposter", R.string.gg_komposter, GegenstandsGruppe.GARTEN, 1f, 1f, 0xFF7A8F5A),
    REGENTONNE("regentonne", R.string.gg_regentonne, GegenstandsGruppe.GARTEN, 0.6f, 0.6f, 0xFF5E7FA0),
    MUELLTONNEN("muelltonnen", R.string.gg_muelltonnen, GegenstandsGruppe.GARTEN, 1.5f, 0.7f, 0xFF6E6E6E),
    WAESCHESPINNE("waeschespinne", R.string.gg_waeschespinne, GegenstandsGruppe.GARTEN, 2f, 2f, 0xFFA0A4A8),
    TRITTSTEIN("trittstein", R.string.gg_trittstein, GegenstandsGruppe.GARTEN, 0.4f, 0.4f, 0xFFAAA8A0),
    MAEHROBOTER("maehroboter", R.string.gg_maehroboter, GegenstandsGruppe.GARTEN, 0.6f, 0.5f, 0xFFE08A2E),

    FINDLING("findling", R.string.gg_findling, GegenstandsGruppe.DEKO, 0.8f, 0.6f, 0xFF8E8E8A),
    STATUE("statue", R.string.gg_statue, GegenstandsGruppe.DEKO, 0.5f, 0.5f, 0xFFB8B8B2),
    VOGELBAD("vogelbad", R.string.gg_vogelbad, GegenstandsGruppe.DEKO, 0.5f, 0.5f, 0xFFA9A9A3),
    LATERNE("laterne", R.string.gg_laterne, GegenstandsGruppe.DEKO, 0.3f, 0.3f, 0xFF3D3D3D),

    // Schmale Gegenstände: Breite = Länge, Höhe = Dicke. Das Muster wiederholt sich entlang der Länge.
    ZAUN("zaun", R.string.gg_zaun, GegenstandsGruppe.ZAUN, 5f, 0.1f, 0xFF9C7A54),
    HECKE("hecke", R.string.gg_hecke, GegenstandsGruppe.ZAUN, 4f, 0.6f, 0xFF5FA05A),
    MAUER("mauer", R.string.gg_mauer, GegenstandsGruppe.ZAUN, 4f, 0.3f, 0xFFA8A8A2),
    BEWAESSERUNG("bewaesserung", R.string.gg_bewaesserung, GegenstandsGruppe.ZAUN, 5f, 0.1f, 0xFF3B7FBF),
    ;

    /** Standardfarbe der Hauptfläche. */
    val farbe: Color = Color(farbeArgb)

    companion object {
        /** Unbekannte Schlüssel (zum Beispiel aus einer späteren Version) werden als Findling gezeigt. */
        fun vonSchluessel(schluessel: String): GegenstandsArt =
            values().firstOrNull { it.schluessel == schluessel } ?: FINDLING
    }
}

/**
 * Zeichnet das Symbol eines Gegenstands in das Rechteck von (0, 0) bis ([breite], [hoehe]) des
 * aktuellen Zeichenbereichs (Draufsicht, Vorderseite oben). Rand und Details leitet die Funktion
 * aus der Hauptfarbe [haupt] ab, so bleibt jedes Symbol bei jeder Farbe lesbar. Die Zeichnungen
 * sind selbst entworfen und bewusst schlicht; sie lassen sich später verfeinern, ohne die
 * Datenbank zu ändern.
 */
fun DrawScope.zeichneGegenstand(
    art: GegenstandsArt,
    breite: Float,
    hoehe: Float,
    haupt: Color,
    dichte: Float,
    pixelProMeter: Float = 0f,
) {
    val b = breite
    val h = hoehe
    val rand = lerp(haupt, Color.Black, 0.45f)
    val dunkel = lerp(haupt, Color.Black, 0.2f)
    val hell = lerp(haupt, Color.White, 0.45f)
    val strich = (1.5f * dichte).coerceAtMost(minOf(b, h) / 4f).coerceAtLeast(0.5f)
    val schwarz = Color(0xFF2B2B2B)
    val wasser = Color(0xFF8FD0EA)
    val gruen = Color(0xFF5FA05A)

    fun rechteck(x: Float, y: Float, w: Float, hh: Float, f: Color, ecke: Float = 0f, umriss: Boolean = true) {
        val ursprung = Offset(x * b, y * h)
        val groesse = Size(w * b, hh * h)
        val radius = CornerRadius(ecke * minOf(b, h))
        drawRoundRect(f, ursprung, groesse, radius)
        if (umriss) drawRoundRect(rand, ursprung, groesse, radius, style = Stroke(strich))
    }

    fun oval(x: Float, y: Float, w: Float, hh: Float, f: Color, umriss: Boolean = true) {
        val ursprung = Offset(x * b, y * h)
        val groesse = Size(w * b, hh * h)
        drawOval(f, ursprung, groesse)
        if (umriss) drawOval(rand, ursprung, groesse, style = Stroke(strich))
    }

    fun ring(x: Float, y: Float, w: Float, hh: Float, f: Color) {
        drawOval(f, Offset(x * b, y * h), Size(w * b, hh * h), style = Stroke(strich))
    }

    fun linie(x1: Float, y1: Float, x2: Float, y2: Float, f: Color = rand) {
        drawLine(f, Offset(x1 * b, y1 * h), Offset(x2 * b, y2 * h), strokeWidth = strich, cap = StrokeCap.Round)
    }

    when (art) {
        GegenstandsArt.HAUS -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.02f)
            rechteck(0f, 0.5f, 1f, 0.5f, dunkel, 0.02f, umriss = false)
            for (i in 1..9) linie(i * 0.1f, 0f, i * 0.1f, 1f, hell.copy(alpha = 0.35f))
            linie(0f, 0.5f, 1f, 0.5f)
            rechteck(0.72f, 0.12f, 0.08f, 0.12f, schwarz, 0f)
            rechteck(0f, 0f, 1f, 1f, Color.Transparent, 0.02f)
        }
        GegenstandsArt.GEBAEUDE_FREI -> {
            // Vorschau im Katalog: ein L-förmiges Gebäude. Im Garten zeichnet GartenScreen die echte Form aus den Punkten.
            val l = Path()
            l.moveTo(0f, 0f)
            l.lineTo(b, 0f)
            l.lineTo(b, 0.5f * h)
            l.lineTo(0.55f * b, 0.5f * h)
            l.lineTo(0.55f * b, h)
            l.lineTo(0f, h)
            l.close()
            drawPath(l, haupt)
            for (i in 1..4) linie(0f, i * 0.2f, 0.55f, i * 0.2f, hell.copy(alpha = 0.5f))
            drawPath(l, rand, style = Stroke(strich))
        }
        GegenstandsArt.GARAGE, GegenstandsArt.SCHUPPEN -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.03f)
            linie(0.5f, 0f, 0.5f, 1f)
            for (i in 1..4) linie(0f, i * 0.2f, 1f, i * 0.2f, hell.copy(alpha = 0.5f))
        }
        GegenstandsArt.CARPORT -> {
            rechteck(0f, 0f, 1f, 1f, haupt.copy(alpha = 0.55f), 0.02f)
            for (i in 1..5) linie(0f, i / 6f, 1f, i / 6f, rand.copy(alpha = 0.6f))
            for ((x, y) in listOf(0f to 0f, 0.9f to 0f, 0f to 0.94f, 0.9f to 0.94f)) {
                rechteck(x, y, 0.1f, 0.06f, schwarz, 0f, umriss = false)
            }
        }
        GegenstandsArt.GEWAECHSHAUS -> {
            rechteck(0f, 0f, 1f, 1f, haupt.copy(alpha = 0.7f), 0.02f)
            linie(0.5f, 0f, 0.5f, 1f)
            for (i in 1..3) linie(i * 0.25f, 0f, i * 0.25f, 1f, hell)
            linie(0f, 0.5f, 1f, 0.5f, hell)
        }
        GegenstandsArt.WAERMEPUMPE -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.08f)
            oval(0.12f, 0.1f, 0.5f, 0.8f, hell)
            linie(0.37f, 0.1f, 0.37f, 0.9f)
            linie(0.12f, 0.5f, 0.62f, 0.5f)
            for (i in 0..3) linie(0.72f, 0.2f + i * 0.2f, 0.92f, 0.2f + i * 0.2f)
        }
        GegenstandsArt.LADESAEULE -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.15f)
            oval(0.3f, 0.2f, 0.4f, 0.6f, hell)
        }
        GegenstandsArt.AUTO -> {
            rechteck(0.04f, 0.14f, 0.12f, 0.17f, schwarz, 0.2f, umriss = false)
            rechteck(0.84f, 0.14f, 0.12f, 0.17f, schwarz, 0.2f, umriss = false)
            rechteck(0.04f, 0.69f, 0.12f, 0.17f, schwarz, 0.2f, umriss = false)
            rechteck(0.84f, 0.69f, 0.12f, 0.17f, schwarz, 0.2f, umriss = false)
            rechteck(0.08f, 0f, 0.84f, 1f, haupt, 0.3f)
            rechteck(0.16f, 0.22f, 0.68f, 0.14f, wasser, 0.1f)
            rechteck(0.16f, 0.66f, 0.68f, 0.1f, wasser, 0.1f)
            rechteck(0.16f, 0.37f, 0.68f, 0.28f, dunkel, 0.08f)
            rechteck(0.12f, 0.03f, 0.18f, 0.05f, Color(0xFFFFF3B0), 0.3f, umriss = false)
            rechteck(0.70f, 0.03f, 0.18f, 0.05f, Color(0xFFFFF3B0), 0.3f, umriss = false)
        }
        GegenstandsArt.MOTORRAD -> {
            rechteck(0.38f, 0f, 0.24f, 0.2f, schwarz, 0.3f, umriss = false)
            rechteck(0.38f, 0.8f, 0.24f, 0.2f, schwarz, 0.3f, umriss = false)
            oval(0.28f, 0.22f, 0.44f, 0.5f, haupt)
            oval(0.4f, 0.55f, 0.2f, 0.25f, schwarz, umriss = false)
            linie(0.05f, 0.24f, 0.95f, 0.24f, schwarz)
        }
        GegenstandsArt.FAHRRAD -> {
            rechteck(0.4f, 0f, 0.2f, 0.28f, schwarz, 0.4f, umriss = false)
            rechteck(0.4f, 0.72f, 0.2f, 0.28f, schwarz, 0.4f, umriss = false)
            linie(0.5f, 0.2f, 0.5f, 0.8f, haupt)
            linie(0.12f, 0.2f, 0.88f, 0.2f, schwarz)
            oval(0.4f, 0.4f, 0.2f, 0.12f, rand, umriss = false)
        }
        GegenstandsArt.TISCH_RUND -> {
            oval(0f, 0f, 1f, 1f, haupt)
            ring(0.14f, 0.14f, 0.72f, 0.72f, hell)
        }
        GegenstandsArt.TISCH_ECKIG -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.06f)
            rechteck(0.08f, 0.1f, 0.84f, 0.8f, Color.Transparent, 0.04f)
            linie(0.2f, 0.5f, 0.8f, 0.5f, hell)
        }
        GegenstandsArt.TISCH_STUEHLE -> {
            val stuhl = lerp(haupt, Color.Black, 0.1f)
            rechteck(0.34f, 0.02f, 0.32f, 0.2f, stuhl, 0.25f)
            rechteck(0.34f, 0.78f, 0.32f, 0.2f, stuhl, 0.25f)
            rechteck(0.02f, 0.34f, 0.2f, 0.32f, stuhl, 0.25f)
            rechteck(0.78f, 0.34f, 0.2f, 0.32f, stuhl, 0.25f)
            rechteck(0.24f, 0.24f, 0.52f, 0.52f, haupt, 0.1f)
            rechteck(0.3f, 0.3f, 0.4f, 0.4f, Color.Transparent, 0.06f)
        }
        GegenstandsArt.STUHL -> {
            rechteck(0.05f, 0.22f, 0.9f, 0.73f, haupt, 0.2f)
            rechteck(0.05f, 0.02f, 0.9f, 0.2f, dunkel, 0.2f)
        }
        GegenstandsArt.BANK -> {
            rechteck(0f, 0.38f, 1f, 0.62f, haupt, 0.08f)
            rechteck(0f, 0f, 1f, 0.34f, dunkel, 0.08f)
            for (i in 1..4) linie(i * 0.2f, 0.38f, i * 0.2f, 1f, hell.copy(alpha = 0.6f))
        }
        GegenstandsArt.LIEGE -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.18f)
            rechteck(0.08f, 0.03f, 0.84f, 0.27f, hell, 0.15f)
            linie(0.1f, 0.5f, 0.9f, 0.5f, hell)
        }
        GegenstandsArt.SONNENSCHIRM -> {
            oval(0f, 0f, 1f, 1f, haupt)
            for (i in 0 until 8) {
                val w = Math.toRadians(i * 45.0)
                linie(0.5f, 0.5f, 0.5f + 0.5f * cos(w).toFloat(), 0.5f + 0.5f * sin(w).toFloat(), hell)
            }
            oval(0.44f, 0.44f, 0.12f, 0.12f, rand, umriss = false)
        }
        GegenstandsArt.GRILL -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.14f, 0.14f, 0.72f, 0.72f, Color(0xFFD8602F), umriss = false)
            for (i in 1..3) linie(0.15f, 0.15f + i * 0.175f, 0.85f, 0.15f + i * 0.175f, schwarz)
        }
        GegenstandsArt.FEUERSTELLE -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.2f, 0.2f, 0.6f, 0.6f, Color(0xFF3A3532))
            oval(0.34f, 0.34f, 0.32f, 0.32f, Color(0xFFF08A2B), umriss = false)
            oval(0.43f, 0.43f, 0.14f, 0.14f, Color(0xFFFFE27A), umriss = false)
        }
        GegenstandsArt.TRAMPOLIN -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.12f, 0.12f, 0.76f, 0.76f, lerp(haupt, Color.White, 0.25f))
            for (i in 0 until 12) {
                val w = Math.toRadians(i * 30.0)
                linie(
                    0.5f + 0.38f * cos(w).toFloat(),
                    0.5f + 0.38f * sin(w).toFloat(),
                    0.5f + 0.5f * cos(w).toFloat(),
                    0.5f + 0.5f * sin(w).toFloat(),
                    hell,
                )
            }
        }
        GegenstandsArt.SANDKASTEN -> {
            rechteck(0f, 0f, 1f, 1f, Color(0xFF9C7A54), 0.05f)
            rechteck(0.1f, 0.1f, 0.8f, 0.8f, haupt, 0.04f)
            oval(0.3f, 0.3f, 0.25f, 0.2f, hell, umriss = false)
        }
        GegenstandsArt.SCHAUKEL -> {
            linie(0f, 0.5f, 1f, 0.5f, haupt)
            drawLine(haupt, Offset(0f, 0.5f * h), Offset(0.12f * b, 0f), strokeWidth = strich * 2, cap = StrokeCap.Round)
            drawLine(haupt, Offset(0f, 0.5f * h), Offset(0.12f * b, h), strokeWidth = strich * 2, cap = StrokeCap.Round)
            drawLine(haupt, Offset(b, 0.5f * h), Offset(0.88f * b, 0f), strokeWidth = strich * 2, cap = StrokeCap.Round)
            drawLine(haupt, Offset(b, 0.5f * h), Offset(0.88f * b, h), strokeWidth = strich * 2, cap = StrokeCap.Round)
            rechteck(0.28f, 0.28f, 0.16f, 0.44f, hell, 0.2f)
            rechteck(0.56f, 0.28f, 0.16f, 0.44f, hell, 0.2f)
        }
        GegenstandsArt.SPIELHAUS -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.06f)
            linie(0f, 0f, 1f, 1f, rand)
            linie(1f, 0f, 0f, 1f, rand)
            rechteck(0.4f, 0.4f, 0.2f, 0.2f, hell, 0.2f)
        }
        GegenstandsArt.POOL -> {
            rechteck(0f, 0f, 1f, 1f, hell, 0.12f)
            rechteck(0.06f, 0.08f, 0.88f, 0.84f, haupt, 0.1f)
            for (i in 1..3) linie(0.15f, 0.2f * i + 0.1f, 0.85f, 0.2f * i + 0.1f, wasser)
        }
        GegenstandsArt.BRUNNEN -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.16f, 0.16f, 0.68f, 0.68f, wasser)
            oval(0.4f, 0.4f, 0.2f, 0.2f, hell)
        }
        GegenstandsArt.HOCHBEET -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.03f)
            rechteck(0.07f, 0.12f, 0.86f, 0.76f, Color(0xFF5A4030), 0.02f, umriss = false)
            for ((x, y) in listOf(0.2f to 0.35f, 0.45f to 0.6f, 0.65f to 0.3f, 0.82f to 0.62f, 0.35f to 0.78f)) {
                oval(x - 0.07f, y - 0.1f, 0.14f, 0.2f, gruen, umriss = false)
            }
        }
        GegenstandsArt.KOMPOSTER -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.04f)
            rechteck(0.12f, 0.12f, 0.76f, 0.76f, Color(0xFF4E3B2C), 0.03f, umriss = false)
            for (i in 1..3) linie(0f, i * 0.25f, 1f, i * 0.25f, hell.copy(alpha = 0.6f))
        }
        GegenstandsArt.REGENTONNE -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.14f, 0.14f, 0.72f, 0.72f, Color(0xFF3C5A78))
        }
        GegenstandsArt.MUELLTONNEN -> {
            val farben = listOf(haupt, Color(0xFFE3C13B), Color(0xFF5FA05A))
            for (i in 0 until 3) {
                rechteck(0.02f + i * 0.33f, 0.04f, 0.29f, 0.92f, farben[i], 0.2f)
                rechteck(0.06f + i * 0.33f, 0.12f, 0.21f, 0.18f, Color.Transparent, 0.2f)
            }
        }
        GegenstandsArt.WAESCHESPINNE -> {
            ring(0.04f, 0.04f, 0.92f, 0.92f, haupt)
            ring(0.28f, 0.28f, 0.44f, 0.44f, haupt)
            for (i in 0 until 8) {
                val w = Math.toRadians(i * 45.0)
                linie(0.5f, 0.5f, 0.5f + 0.46f * cos(w).toFloat(), 0.5f + 0.46f * sin(w).toFloat(), dunkel)
            }
            oval(0.45f, 0.45f, 0.1f, 0.1f, rand, umriss = false)
        }
        GegenstandsArt.TRITTSTEIN -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.2f, 0.2f, 0.35f, 0.3f, hell, umriss = false)
        }
        GegenstandsArt.MAEHROBOTER -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.5f)
            oval(0.4f, 0.12f, 0.2f, 0.2f, hell)
            rechteck(0.2f, 0.62f, 0.6f, 0.12f, schwarz, 0.3f, umriss = false)
        }
        GegenstandsArt.FINDLING -> {
            oval(0f, 0.05f, 1f, 0.9f, haupt)
            oval(0.2f, 0.2f, 0.35f, 0.28f, hell, umriss = false)
        }
        GegenstandsArt.STATUE -> {
            rechteck(0f, 0f, 1f, 1f, haupt, 0.1f)
            oval(0.2f, 0.2f, 0.6f, 0.6f, hell)
        }
        GegenstandsArt.VOGELBAD -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.14f, 0.14f, 0.72f, 0.72f, wasser)
            oval(0.4f, 0.45f, 0.2f, 0.15f, Color(0xFF8A5A3C), umriss = false)
        }
        GegenstandsArt.LATERNE -> {
            oval(0f, 0f, 1f, 1f, haupt)
            oval(0.28f, 0.28f, 0.44f, 0.44f, Color(0xFFFFE27A), umriss = false)
        }
        GegenstandsArt.ZAUN -> {
            // Latten dicht an dicht, alle 1 m ein Pfosten
            val meter = if (pixelProMeter > 0f) pixelProMeter else b / art.breiteM
            rechteck(0f, 0f, 1f, 1f, haupt, 0f)
            if (b / meter <= 400f) {
                val latte = meter / 6f
                if (latte >= 2f * dichte && b / latte <= 1500f) {
                    var x = latte
                    while (x < b) {
                        drawLine(hell.copy(alpha = 0.7f), Offset(x, 0f), Offset(x, h), strokeWidth = strich * 0.7f)
                        x += latte
                    }
                }
                var x = 0f
                val pfosten = maxOf(h * 1.5f, 3f * dichte)
                while (x <= b + 0.01f) {
                    drawRect(rand, Offset(x - pfosten / 2f, h / 2f - pfosten / 2f), Size(pfosten, pfosten))
                    x += meter
                }
            }
        }
        GegenstandsArt.HECKE -> {
            // Rundliche Büsche nebeneinander in zwei Grüntönen
            rechteck(0f, 0f, 1f, 1f, dunkel, 0.45f, umriss = false)
            val busch = maxOf(h * 0.9f, 2f * dichte)
            if (b / busch <= 800f) {
                var x = busch / 2f
                var i = 0
                while (x < b) {
                    drawCircle(if (i % 2 == 0) haupt else hell, radius = h * 0.46f, center = Offset(x, h / 2f))
                    x += busch
                    i++
                }
            }
            rechteck(0f, 0f, 1f, 1f, Color.Transparent, 0.45f)
        }
        GegenstandsArt.MAUER -> {
            // Steine: senkrechte Fugen, bei dickeren Mauern zusätzlich eine Lagerfuge in der Mitte
            rechteck(0f, 0f, 1f, 1f, haupt, 0f)
            val stein = maxOf(h * 2.2f, 4f * dichte)
            if (b / stein <= 800f) {
                var x = stein
                var i = 0
                while (x < b) {
                    if (h > 8f * dichte) {
                        val oben = i % 2 == 0
                        drawLine(rand, Offset(x, if (oben) 0f else h / 2f), Offset(x, if (oben) h / 2f else h), strokeWidth = strich * 0.8f)
                        drawLine(rand, Offset(x - stein / 2f, if (oben) h / 2f else 0f), Offset(x - stein / 2f, if (oben) h else h / 2f), strokeWidth = strich * 0.8f)
                    } else {
                        drawLine(rand, Offset(x, 0f), Offset(x, h), strokeWidth = strich * 0.8f)
                    }
                    x += stein
                    i++
                }
                if (h > 8f * dichte) drawLine(rand, Offset(0f, h / 2f), Offset(b, h / 2f), strokeWidth = strich * 0.8f)
            }
        }
        GegenstandsArt.BEWAESSERUNG -> {
            // Gestrichelte Leitung, alle 2 m ein Regner
            val meter = if (pixelProMeter > 0f) pixelProMeter else b / art.breiteM
            val dicke = maxOf(strich * 1.4f, h * 0.6f)
            val strichLaenge = maxOf(meter / 6f, 3f * dichte)
            drawLine(
                haupt,
                Offset(0f, h / 2f),
                Offset(b, h / 2f),
                strokeWidth = dicke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(strichLaenge, strichLaenge * 0.6f)),
            )
            if (b / (2f * meter) <= 300f) {
                val regner = maxOf(h * 1.6f, 3.5f * dichte)
                var x = meter
                while (x < b) {
                    drawCircle(wasser, radius = regner, center = Offset(x, h / 2f))
                    drawCircle(rand, radius = regner, center = Offset(x, h / 2f), style = Stroke(strich))
                    x += 2f * meter
                }
            }
        }
    }
}
