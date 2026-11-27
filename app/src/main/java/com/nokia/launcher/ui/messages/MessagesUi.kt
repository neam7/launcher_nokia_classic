package com.nokia.launcher.ui.messages

import com.nokia.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nokia.launcher.data.Sms
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import java.text.DateFormat
import java.util.Date

/** One conversation as plain text, oldest first; [messages] come newest first. */
@Composable
fun ColumnScope.ThreadContent(title: String, messages: List<Sms>) {
    val ink = LocalPhoneColors.current.lcdText
    val me = stringResource(R.string.me)
    TitleBar(title, messages.size.toString())
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true) {
        items(messages) { sms ->
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                LcdText("${if (sms.outgoing) "▶ $me" else "◀ $title"}  ${date.format(Date(sms.time))}", small = true)
                LcdText(sms.body, Modifier.padding(vertical = 4.dp), maxLines = Int.MAX_VALUE)
                Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.25f)))
            }
        }
    }
}

@Preview(widthDp = 380, heightDp = 440)
@Composable
private fun ThreadPreview() {
    LauncherTheme {
        Lcd(emptyList()) {
            ThreadContent("Ana", listOf(Sms(1, "5550100", "Voy", 0, true), Sms(1, "5550100", "¿Vienes?", 0, false)))
        }
    }
}
