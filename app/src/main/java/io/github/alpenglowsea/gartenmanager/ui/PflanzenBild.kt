package io.github.alpenglowsea.gartenmanager.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.BildAusschnitt
import io.github.alpenglowsea.gartenmanager.daten.BildInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** Kleiner Zwischenspeicher für gelesene Bilder (ca. 24 MB), damit das Scrollen in der Liste flüssig bleibt. */
private object BildCache {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun hole(schluessel: String): ImageBitmap? = cache.get(schluessel)
    fun lege(schluessel: String, bild: ImageBitmap) {
        cache.put(schluessel, bild)
    }
}

/**
 * Liest ein Bild aus den Assets und verkleinert es beim Lesen so, dass die kürzere Seite nicht kleiner
 * als [maxKante] Pixel wird. Gibt null zurück, wenn das Bild fehlt oder nicht lesbar ist.
 */
internal fun ladeBild(context: Context, datei: String, maxKante: Int): ImageBitmap? {
    val schluessel = "$datei@$maxKante"
    BildCache.hole(schluessel)?.let { return it }
    return try {
        val assets = context.applicationContext.assets
        val grenzen = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open("bilder/$datei").use { BitmapFactory.decodeStream(it, null, grenzen) }
        var stufe = 1
        while (grenzen.outWidth / (stufe * 2) >= maxKante && grenzen.outHeight / (stufe * 2) >= maxKante) stufe *= 2
        val optionen = BitmapFactory.Options().apply { inSampleSize = stufe }
        val bitmap = assets.open("bilder/$datei").use { BitmapFactory.decodeStream(it, null, optionen) }
        if (bitmap == null) {
            null
        } else {
            val bild = bitmap.asImageBitmap()
            BildCache.lege(schluessel, bild)
            bild
        }
    } catch (e: Exception) {
        null
    }
}

/** Lädt ein Bild im Hintergrund. Solange es nicht da ist (oder [datei] null ist), liefert es null. */
@Composable
internal fun rememberPflanzenBild(datei: String?, maxKante: Int): ImageBitmap? {
    val context = LocalContext.current
    val zustand = produceState<ImageBitmap?>(initialValue = null, datei, maxKante) {
        value = if (datei == null) null else withContext(Dispatchers.IO) { ladeBild(context, datei, maxKante) }
    }
    return zustand.value
}

/** Seitenverhältnisse (Breite durch Höhe) der beiden Rahmen, in denen ein Foto gezeigt wird. */
internal const val KACHEL_SEITEN = 1f
internal const val KOPF_SEITEN = 1.6f

/**
 * Welcher Teil eines Bildes ([bw] mal [bh] Pixel) in einem Rahmen mit Seitenverhältnis [seiten] zu sehen ist.
 * Zoom 1 ist der größte Ausschnitt, der den Rahmen noch ausfüllt. Der Bereich bleibt immer im Bild.
 */
internal fun ausschnittBereich(bw: Float, bh: Float, seiten: Float, mitteX: Float, mitteY: Float, zoom: Float): Rect {
    val grundBreite: Float
    val grundHoehe: Float
    if (bw / bh > seiten) {
        grundHoehe = bh
        grundBreite = bh * seiten
    } else {
        grundBreite = bw
        grundHoehe = bw / seiten
    }
    val b = grundBreite / zoom
    val h = grundHoehe / zoom
    val links = (mitteX * bw - b / 2f).coerceIn(0f, maxOf(0f, bw - b))
    val oben = (mitteY * bh - h / 2f).coerceIn(0f, maxOf(0f, bh - h))
    return Rect(links, oben, links + b, oben + h)
}

/** Begrenzt Mittelpunkt und Zoom so, dass beide Rahmen vollständig im Bild bleiben. */
internal fun begrenzeAusschnitt(bw: Float, bh: Float, mitteX: Float, mitteY: Float, zoom: Float): Triple<Float, Float, Float> {
    val z = zoom.coerceIn(1f, 5f)
    var halbX = 0f
    var halbY = 0f
    for (seiten in listOf(KACHEL_SEITEN, KOPF_SEITEN)) {
        val r = ausschnittBereich(bw, bh, seiten, 0.5f, 0.5f, z)
        halbX = maxOf(halbX, r.width / bw / 2f)
        halbY = maxOf(halbY, r.height / bh / 2f)
    }
    val x = if (halbX >= 0.5f) 0.5f else mitteX.coerceIn(halbX, 1f - halbX)
    val y = if (halbY >= 0.5f) 0.5f else mitteY.coerceIn(halbY, 1f - halbY)
    return Triple(x, y, z)
}

/** Zeichnet den gewählten Ausschnitt eines Bildes so, dass er den ganzen Platz ausfüllt. */
@Composable
internal fun AusschnittBild(bild: ImageBitmap, ausschnitt: BildAusschnitt?, beschreibung: String, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = beschreibung }) {
        val bereich = ausschnittBereich(
            bild.width.toFloat(), bild.height.toFloat(), size.width / size.height,
            ausschnitt?.mitteX ?: 0.5f, ausschnitt?.mitteY ?: 0.5f, ausschnitt?.zoom ?: 1f,
        )
        drawImage(
            image = bild,
            srcOffset = IntOffset(bereich.left.roundToInt(), bereich.top.roundToInt()),
            srcSize = IntSize(bereich.width.roundToInt().coerceAtLeast(1), bereich.height.roundToInt().coerceAtLeast(1)),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/** Kachelbild im Katalog: das Foto (quadratisch aus der Mitte), ohne Foto das Piktogramm. */
@Composable
fun PflanzenKachelBild(
    bild: String?,
    gruppe: String,
    name: String,
    modifier: Modifier = Modifier,
    ausschnitt: BildAusschnitt? = null,
) {
    // Die kürzere Seite bleibt mindestens 320 Pixel, damit auch ein hineingezoomter Ausschnitt scharf ist
    val bmp = rememberPflanzenBild(bild, 320)
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            AusschnittBild(bmp, ausschnitt, name, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
        } else {
            PflanzenSymbol(gruppe, name, 44.dp)
        }
    }
}

/** Breites Bild oben im Info-Fenster (Ausschnitt aus der Mitte) mit kleinem Knopf zur Vollansicht. */
@Composable
internal fun PflanzenKopfbild(bild: BildInfo, name: String, ausschnitt: BildAusschnitt?, onVoll: () -> Unit) {
    val bmp = rememberPflanzenBild(bild.datei, 1000)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(KOPF_SEITEN)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (bmp != null) {
            AusschnittBild(bmp, ausschnitt, name, Modifier.fillMaxSize())
        }
        IconButton(
            onClick = onVoll,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_vollbild),
                contentDescription = stringResource(R.string.info_bild_gross),
                tint = Color.White,
            )
        }
    }
}

/** Kurze Namensnennung unter dem Bild. */
@Composable
internal fun fotoZeile(b: BildInfo): String =
    if (b.urheber.isNullOrBlank()) stringResource(R.string.info_foto_ohne, b.lizenz)
    else stringResource(R.string.info_foto_mit, b.urheber, b.lizenz)

/** Vollständiger Nachweis zu einem Foto: Urheber, Lizenz, Links zur Quelle. */
@Composable
internal fun BildNachweis(b: BildInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.info_bild_titel), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (!b.urheber.isNullOrBlank()) {
            Text(stringResource(R.string.info_bild_urheber, b.urheber), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.info_lizenz, b.lizenz), style = MaterialTheme.typography.bodySmall)
        val lizenzLink = b.lizenzUrl ?: lizenzAdresse(b.lizenz)
        if (lizenzLink != null) LinkText(lizenzLink)
        if (b.seite != null) LinkText(b.seite)
        if (b.titel != null) Text(stringResource(R.string.info_bild_datei, b.titel), style = MaterialTheme.typography.bodySmall)
        if (b.abruf != null) Text(stringResource(R.string.info_abruf, b.abruf), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Das ganze Bild ohne Zuschnitt. Zoomen mit zwei Fingern, Doppeltipp setzt zurück.
 * Von hier aus lässt sich der Ausschnitt für Kachel und Info-Fenster anpassen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BildVollansicht(
    bild: BildInfo,
    name: String,
    ausschnitt: BildAusschnitt?,
    onSpeichern: (mitteX: Float, mitteY: Float, zoom: Float) -> Unit,
    onZuruecksetzen: () -> Unit,
    onZurueck: () -> Unit,
) {
    var bearbeiten by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (bearbeiten) bearbeiten = false else onZurueck() }
    val bmp = rememberPflanzenBild(bild.datei, 2400)
    var zoom by remember { mutableFloatStateOf(1f) }
    var versatz by remember { mutableStateOf(Offset.Zero) }
    var flaeche by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (bearbeiten) stringResource(R.string.ausschnitt_titel) else name) },
                navigationIcon = {
                    IconButton(onClick = { if (bearbeiten) bearbeiten = false else onZurueck() }) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
    ) { innen ->
        if (bearbeiten && bmp != null) {
            AusschnittEditor(
                bild = bmp,
                name = name,
                start = ausschnitt,
                onSpeichern = { x, y, z ->
                    onSpeichern(x, y, z)
                    bearbeiten = false
                },
                onZuruecksetzen = {
                    onZuruecksetzen()
                    bearbeiten = false
                },
                onAbbrechen = { bearbeiten = false },
                modifier = Modifier.padding(innen).fillMaxSize(),
            )
        } else {
            Column(Modifier.padding(innen).fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                        .clipToBounds()
                        .onSizeChanged { flaeche = it }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                zoom = 1f
                                versatz = Offset.Zero
                            })
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, gezoomt, _ ->
                                val neu = (zoom * gezoomt).coerceIn(1f, 5f)
                                val grenzeX = flaeche.width * (neu - 1f) / 2f
                                val grenzeY = flaeche.height * (neu - 1f) / 2f
                                zoom = neu
                                versatz = Offset(
                                    (versatz.x + pan.x).coerceIn(-grenzeX, grenzeX),
                                    (versatz.y + pan.y).coerceIn(-grenzeY, grenzeY),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = versatz.x
                                translationY = versatz.y
                            },
                        )
                    }
                }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = { bearbeiten = true }, enabled = bmp != null) {
                        Text(stringResource(R.string.ausschnitt_anpassen))
                    }
                    BildNachweis(bild)
                    Text(stringResource(R.string.info_bild_zoomhinweis), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Ausschnitt festlegen: Das ganze Bild mit zwei Rahmen (weiß = Kachel, gelb = Info-Fenster). Ein Finger
 * verschiebt die Rahmen, zwei Finger zoomen. Darunter eine Vorschau von beiden.
 */
@Composable
private fun AusschnittEditor(
    bild: ImageBitmap,
    name: String,
    start: BildAusschnitt?,
    onSpeichern: (Float, Float, Float) -> Unit,
    onZuruecksetzen: () -> Unit,
    onAbbrechen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bw = bild.width.toFloat()
    val bh = bild.height.toFloat()
    val anfang = begrenzeAusschnitt(bw, bh, start?.mitteX ?: 0.5f, start?.mitteY ?: 0.5f, start?.zoom ?: 1f)
    var mitteX by remember { mutableFloatStateOf(anfang.first) }
    var mitteY by remember { mutableFloatStateOf(anfang.second) }
    var zoom by remember { mutableFloatStateOf(anfang.third) }
    var flaeche by remember { mutableStateOf(IntSize.Zero) }
    val vorschau = BildAusschnitt(0, mitteX, mitteY, zoom)

    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .onSizeChanged { flaeche = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gezoomt, _ ->
                        val skala = min(flaeche.width / bw, flaeche.height / bh)
                        if (skala > 0f) {
                            val neu = begrenzeAusschnitt(
                                bw, bh,
                                mitteX + pan.x / (bw * skala),
                                mitteY + pan.y / (bh * skala),
                                zoom * gezoomt,
                            )
                            mitteX = neu.first
                            mitteY = neu.second
                            zoom = neu.third
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val skala = min(size.width / bw, size.height / bh)
                val anzeigeB = bw * skala
                val anzeigeH = bh * skala
                val ox = (size.width - anzeigeB) / 2f
                val oy = (size.height - anzeigeH) / 2f
                drawImage(
                    image = bild,
                    dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
                    dstSize = IntSize(anzeigeB.roundToInt(), anzeigeH.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
                val rahmen = listOf(KOPF_SEITEN to Color(0xFFFFD54F), KACHEL_SEITEN to Color.White)
                for ((seiten, farbe) in rahmen) {
                    val r = ausschnittBereich(bw, bh, seiten, mitteX, mitteY, zoom)
                    drawRect(
                        color = farbe,
                        topLeft = Offset(ox + r.left * skala, oy + r.top * skala),
                        size = Size(r.width * skala, r.height * skala),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.ausschnitt_hinweis), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                AusschnittBild(bild, vorschau, name, Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)))
                AusschnittBild(bild, vorschau, name, Modifier.width(128.dp).height(80.dp).clip(RoundedCornerShape(10.dp)))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAbbrechen) { Text(stringResource(R.string.ausschnitt_abbrechen)) }
                TextButton(onClick = onZuruecksetzen) { Text(stringResource(R.string.ausschnitt_zuruecksetzen)) }
                Button(onClick = { onSpeichern(mitteX, mitteY, zoom) }) { Text(stringResource(R.string.ausschnitt_speichern)) }
            }
        }
    }
}
