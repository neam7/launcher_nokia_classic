package com.nokia.launcher.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors

/** The plain information screen of a classic phone. */
@Composable
fun ColumnScope.AboutContent(version: String) {
    TitleBar(stringResource(R.string.about))
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Line(stringResource(R.string.app_name), 20.sp, bold = true, top = 14)
        Line(stringResource(R.string.about_version, version), 15.sp, top = 4)
        Rule()
        Line(stringResource(R.string.about_made_by), 13.sp)
        // A person's name: the same in every language.
        Line("Neftalí Angeles Montaño", 16.sp, bold = true, top = 2)
        Rule()
        Line(stringResource(R.string.about_what), 13.sp)
        Line(stringResource(R.string.about_notice), 12.sp, top = 10)
    }
}

@Composable
private fun Line(text: String, size: TextUnit, bold: Boolean = false, top: Int = 0) {
    Text(
        text, Modifier.fillMaxWidth().padding(top = top.dp),
        color = LocalPhoneColors.current.lcdText, fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = size,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Rule() {
    Box(Modifier.fillMaxWidth(0.6f).padding(vertical = 10.dp).height(1.dp).background(LocalPhoneColors.current.lcdText))
}

@Preview(widthDp = 380, heightDp = 440)
@Composable
private fun AboutPreview() {
    LauncherTheme { Lcd(emptyList()) { AboutContent("1.0") } }
}
