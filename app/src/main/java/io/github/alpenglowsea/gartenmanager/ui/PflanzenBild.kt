package io.github.alpenglowsea.gartenmanager.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.alpenglowsea.gartenmanager.R
import io.github.alpenglowsea.gartenmanager.daten.BildInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
private fun ladeBild(context: Context, datei: String, maxKante: Int): ImageBitmap? {
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

/** Kachelbild im Katalog: das Foto (quadratisch aus der Mitte), ohne Foto das Piktogramm. */
@Composable
fun PflanzenKachelBild(bild: String?, gruppe: String, name: String, modifier: Modifier = Modifier) {
    val bmp = rememberPflanzenBild(bild, 192)
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
            )
        } else {
            PflanzenSymbol(gruppe, name, 44.dp)
        }
    }
}

/** Breites Bild oben im Info-Fenster (Ausschnitt aus der Mitte) mit kleinem Knopf zur Vollansicht. */
@Composable
internal fun PflanzenKopfbild(bild: BildInfo, name: String, onVoll: () -> Unit) {
    val bmp = rememberPflanzenBild(bild.datei, 1000)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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

/** Das ganze Bild ohne Zuschnitt. Zoomen mit zwei Fingern, Doppeltipp setzt zurück. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BildVollansicht(bild: BildInfo, name: String, onZurueck: () -> Unit) {
    BackHandler(onBack = onZurueck)
    val bmp = rememberPflanzenBild(bild.datei, 2400)
    var zoom by remember { mutableFloatStateOf(1f) }
    var versatz by remember { mutableStateOf(Offset.Zero) }
    var flaeche by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(painterResource(R.drawable.ic_zurueck), contentDescription = stringResource(R.string.zurueck))
                    }
                },
            )
        },
    ) { innen ->
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
                BildNachweis(bild)
                Text(stringResource(R.string.info_bild_zoomhinweis), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
