package com.nokia.launcher.ui.appdrawer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.home.Lcd
import androidx.compose.ui.tooling.preview.Preview
import com.nokia.launcher.ui.home.CursorText
import androidx.core.graphics.drawable.toBitmap
import com.nokia.launcher.R
import com.nokia.launcher.ui.theme.LocalPhoneColors
import java.text.Normalizer

data class AppEntry(val label: String, val component: ComponentName, val icon: ImageBitmap)

/** Blocking: call off the main thread. */
fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(query, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            AppEntry(
                label = it.loadLabel(pm).toString(),
                component = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                icon = it.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
            )
        }
        .sortedBy { searchKey(it.label) }
}

private val accents = Regex("\\p{Mn}+")

/** Lowercase without accents, so "camara" finds "Cámara". */
fun searchKey(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD).replace(accents, "").lowercase()

fun filterApps(apps: List<AppEntry>, text: String): List<AppEntry> {
    val wanted = searchKey(text)
    return apps.filter { searchKey(it.label).contains(wanted) }
}

/** The app menu as in reference image 11: color icons in pages with dots, plus a search field. */
@Composable
fun ColumnScope.AppMenu(
    apps: List<AppEntry>,
    search: String,
    pending: Boolean = false,
    onHold: (AppEntry) -> Unit = {},
    onOpen: (AppEntry) -> Unit,
) {
    val colors = LocalPhoneColors.current
    CursorText(
        stringResource(R.string.search_field, search),
        colors.lcdText,
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(colors.lcdText.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .border(1.dp, colors.lcdText.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        pending = pending,
        fontSize = 14.sp,
        maxLines = 1,
    )
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val dots = 14.dp
        val columns = (maxWidth / 80.dp).toInt().coerceAtLeast(3)
        val rows = ((maxHeight - dots) / 76.dp).toInt().coerceAtLeast(1)
        val iconSize = min(48.dp, (maxHeight - dots) / rows * 0.56f)
        val pages = apps.chunked(columns * rows)
        val pager = rememberPagerState { pages.size }
        LaunchedEffect(search) { if (pages.isNotEmpty()) pager.scrollToPage(0) }
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                val items = pages.getOrElse(page) { emptyList() }
                Column(Modifier.fillMaxSize()) {
                    repeat(rows) { row ->
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            repeat(columns) { column ->
                                val index = row * columns + column
                                val app = items.getOrNull(index)
                                if (app == null) {
                                    Spacer(Modifier.weight(1f))
                                } else {
                                    // The highlighted app is the one `Open` launches.
                                    AppTile(
                                        app, page == 0 && index == 0, iconSize,
                                        Modifier.weight(1f).fillMaxHeight(),
                                        { onHold(app) },
                                    ) { onOpen(app) }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(dots),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Past 12 pages the dots stop fitting, so show numbers.
                if (pages.size > 12) {
                    Text("${pager.currentPage + 1}/${pages.size}", color = colors.lcdText, fontSize = 10.sp)
                } else {
                    repeat(pages.size) {
                        val selected = it == pager.currentPage
                        Box(
                            Modifier.padding(horizontal = 3.dp).size(if (selected) 8.dp else 5.dp)
                                .background(colors.lcdText.copy(alpha = if (selected) 1f else 0.4f), CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: AppEntry,
    selected: Boolean,
    iconSize: Dp,
    modifier: Modifier,
    onHold: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalPhoneColors.current
    val shape = RoundedCornerShape(10.dp)
    val highlight = if (selected) {
        Modifier.background(colors.lcdText.copy(alpha = 0.22f)).border(1.5.dp, colors.lcdText, shape)
    } else {
        Modifier
    }
    Column(
        modifier.padding(2.dp).clip(shape).then(highlight).combinedClickable(onLongClick = onHold, onClick = onClick).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(app.icon, contentDescription = null, Modifier.size(iconSize))
        Text(
            app.label,
            Modifier.padding(top = 3.dp),
            color = colors.lcdText,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 380, heightDp = 440)
@Composable
private fun AppMenuPreview() {
    val app = AppEntry("Camera", ComponentName("a", "b"), ImageBitmap(96, 96))
    LauncherTheme { Lcd(emptyList()) { AppMenu(List(14) { app }, "") {} } }
}
