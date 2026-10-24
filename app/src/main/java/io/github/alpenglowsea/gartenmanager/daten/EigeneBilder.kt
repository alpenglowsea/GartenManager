package io.github.alpenglowsea.gartenmanager.daten

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import java.io.File

/** Kennzeichnung der Lizenzzeile für Fotos, die der Nutzer selbst hinzugefügt hat. */
const val LIZENZ_EIGEN = "eigenes Foto"

/**
 * Fotos, die der Nutzer selbst zu einer Pflanze hinzufügt (für eigene Pflanzen und für Pflanzen ohne Foto).
 * Sie liegen nur auf dem Gerät im privaten Speicher der App (Ordner eigene_bilder, eine Datei je Pflanzennummer).
 * Ein Foto aus den Grunddaten wird nie überschrieben.
 */
object EigeneBilder {
    /** Dateinamen-Vorsatz; daran erkennt das Bild-Laden, dass die Datei nicht aus den Assets kommt. */
    const val VORSATZ = "eigen:"

    /** Zählt hoch, wenn ein Foto hinzukommt oder entfällt; Bildschirme lesen den Wert, um sich neu zu zeichnen. */
    val stand = mutableIntStateOf(0)

    private const val LAENGSTE_KANTE = 2000

    fun dateiname(pflanzeId: Long): String = "$VORSATZ$pflanzeId.jpg"

    internal fun ordner(context: Context): File =
        File(context.applicationContext.filesDir, "eigene_bilder").also { it.mkdirs() }

    fun datei(context: Context, dateiname: String): File =
        File(ordner(context), dateiname.removePrefix(VORSATZ))

    /** Alle vorhandenen eigenen Fotos: Pflanzennummer -> Dateiname. */
    fun alle(context: Context): Map<Long, String> {
        val karte = HashMap<Long, String>()
        ordner(context).listFiles()?.forEach { f ->
            val id = f.name.removeSuffix(".jpg").toLongOrNull()
            if (id != null && f.length() > 0) karte[id] = dateiname(id)
        }
        return karte
    }

    /** Angaben für das Info-Fenster: das Foto stammt vom Nutzer, es gibt keine fremde Lizenz. */
    fun bildInfo(pflanzeId: Long): BildInfo = BildInfo(
        datei = dateiname(pflanzeId),
        urheber = null,
        lizenz = LIZENZ_EIGEN,
        lizenzUrl = null,
        seite = null,
        titel = null,
        herkunft = null,
        abruf = null,
    )

    /**
     * Liest das gewählte Bild, verkleinert es (längste Seite höchstens 2000 Pixel), dreht es nach den
     * Aufnahmeangaben richtig herum und speichert es als JPEG. Gibt false zurück, wenn es nicht lesbar ist.
     * Muss in einem Hintergrund-Thread laufen.
     */
    fun speichere(context: Context, pflanzeId: Long, quelle: Uri): Boolean {
        return try {
            val aufloeser = context.applicationContext.contentResolver
            val grenzen = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // Beim reinen Ausmessen liefert decodeStream absichtlich kein Bild (null) - nur der Stream selbst muss da sein
            val stream = aufloeser.openInputStream(quelle) ?: return false
            stream.use { BitmapFactory.decodeStream(it, null, grenzen) }
            if (grenzen.outWidth <= 0 || grenzen.outHeight <= 0) return false
            var stufe = 1
            while (maxOf(grenzen.outWidth, grenzen.outHeight) / (stufe * 2) >= LAENGSTE_KANTE) stufe *= 2
            val optionen = BitmapFactory.Options().apply { inSampleSize = stufe }
            val roh = aufloeser.openInputStream(quelle)?.use { BitmapFactory.decodeStream(it, null, optionen) } ?: return false
            val drehung = try {
                aufloeser.openInputStream(quelle)?.use { st ->
                    when (ExifInterface(st).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                0f // ohne lesbare Drehangabe das Bild so nehmen, wie es ist
            }
            val bild = if (drehung == 0f) roh else {
                Bitmap.createBitmap(roh, 0, 0, roh.width, roh.height, Matrix().apply { postRotate(drehung) }, true)
            }
            val ziel = datei(context, dateiname(pflanzeId))
            val vorlaeufig = File(ziel.parentFile, ziel.name + ".neu")
            vorlaeufig.outputStream().use { bild.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (!vorlaeufig.renameTo(ziel)) {
                ziel.delete()
                if (!vorlaeufig.renameTo(ziel)) return false
            }
            stand.intValue = stand.intValue + 1
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    fun loesche(context: Context, pflanzeId: Long) {
        datei(context, dateiname(pflanzeId)).delete()
        stand.intValue = stand.intValue + 1
    }
}
