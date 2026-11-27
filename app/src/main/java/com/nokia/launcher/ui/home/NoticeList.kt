package com.nokia.launcher.ui.home

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokia.launcher.R
import com.nokia.launcher.data.Notice
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** All the notices, scrollable. One is removed with its ✕ or by sliding it aside. */
@Composable
fun NoticeList(
    notices: List<Notice>,
    onOpen: (Notice) -> Unit,
    onDismiss: (Notice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberLazyListState()
    // The list would keep the old first row in place, hiding a new one above it.
    LaunchedEffect(notices.firstOrNull()?.key) { scroll.scrollToItem(0) }
    LazyColumn(modifier, state = scroll, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(notices, key = { it.key }) { NoticeItem(it, { onOpen(it) }, { onDismiss(it) }) }
    }
}

@Composable
private fun NoticeItem(notice: Notice, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val ink = LocalPhoneColors.current.lcdText
    var slide by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    // Android keeps some notices; those do not slide.
    val sliding = if (notice.clearable) {
        Modifier.draggable(
            rememberDraggableState { slide += it },
            Orientation.Horizontal,
            onDragStopped = { if (abs(slide) > width / 3) onDismiss() else slide = 0f },
        )
    } else {
        Modifier
    }
    Row(
        Modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat() }
            .offset { IntOffset(slide.roundToInt(), 0) }
            .alpha(1f - (abs(slide) / width).coerceAtMost(0.7f))
            .then(sliding)
            .border(1.dp, ink, RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 6.dp, vertical = 4.dp)) {
            NoticeLine("✉ ${notice.app}" + if (notice.title.isBlank()) "" else ": ${notice.title}", true)
            NoticeLine(notice.text, false)
        }
        if (notice.clearable) {
            val remove = stringResource(R.string.notices_remove)
            Text(
                "✕",
                Modifier.clickable(onClick = onDismiss).semantics { contentDescription = remove }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                color = ink, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun NoticeLine(text: String, bold: Boolean) {
    Text(
        text, color = LocalPhoneColors.current.lcdText, fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

@Preview(name = "Home with notices", widthDp = 380, heightDp = 440)
@Composable
private fun NoticesPreview() {
    val notices = List(8) { Notice("$it", "Mensajes", "Ana", "Nos vemos a las 8", clearable = it != 1) }
    LauncherTheme { Lcd(emptyList()) { HomeContent("", notices) } }
}
