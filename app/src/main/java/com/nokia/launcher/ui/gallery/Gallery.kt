package com.nokia.launcher.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Needs access to images. Newest first. */
fun loadPhotos(context: Context, limit: Int = 300): List<Uri> = runCatching {
    val table = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    context.contentResolver.query(
        table, arrayOf(MediaStore.Images.Media._ID), null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { c ->
        buildList { while (size < limit && c.moveToNext()) add(ContentUris.withAppendedId(table, c.getLong(0))) }
    }
}.getOrNull().orEmpty()

/** A downscaled bitmap, loaded off the main thread; null while loading or if the file is gone. */
@Composable
private fun photo(uri: Uri, pixels: Int, thumbnail: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (thumbnail) {
                    context.contentResolver.loadThumbnail(uri, Size(pixels, pixels), null)
                } else {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                        val largest = maxOf(info.size.width, info.size.height)
                        if (largest > pixels) decoder.setTargetSampleSize(largest / pixels + 1)
                    }
                }.asImageBitmap()
            }.getOrNull()
        }
    }
    return image
}

@Composable
fun ColumnScope.GalleryGrid(photos: List<Uri>, onOpen: (Int) -> Unit) {
    val ink = LocalPhoneColors.current.lcdText
    TitleBar(stringResource(R.string.menu_gallery), photos.size.toString())
    LazyVerticalGrid(GridCells.Adaptive(96.dp), Modifier.weight(1f).padding(top = 4.dp)) {
        itemsIndexed(photos) { index, uri ->
            Box(Modifier.padding(2.dp).aspectRatio(1f).border(1.dp, ink).clickable { onOpen(index) }) {
                photo(uri, 256, thumbnail = true)?.let {
                    Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PhotoContent(photos: List<Uri>, index: Int) {
    TitleBar(stringResource(R.string.menu_gallery), "${index + 1}/${photos.size}")
    Box(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
        val uri = photos.getOrNull(index) ?: return@Box
        val image = photo(uri, 1600, thumbnail = false)
        if (image == null) {
            LcdText("…")
        } else {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Preview(widthDp = 380, heightDp = 440)
@Composable
private fun GalleryPreview() {
    LauncherTheme { Lcd(emptyList()) { GalleryGrid(emptyList()) {} } }
}
