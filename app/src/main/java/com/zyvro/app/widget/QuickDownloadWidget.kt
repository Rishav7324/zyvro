package com.zyvro.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.Button
import androidx.glance.LocalContext

/**
 * Widget 1 — Quick Paste & Go (2x2).
 * Opens Home; user pastes link and fetches. ACTION_SEND from other apps
 * already deep-links into Home via MainActivity.
 */
class QuickDownloadWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickContent()
            }
        }
    }

    @Composable
    private fun QuickContent() {
        val context = LocalContext.current
        Column(
            modifier = androidx.glance.GlanceModifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Zyvro Quick Download",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
            Text(
                text = "Paste any video link & fetch",
                style = TextStyle(fontSize = 12.sp)
            )
            Spacer(modifier = androidx.glance.GlanceModifier.height(8.dp))
            Button(
                text = "Paste & Go",
                onClick = actionStartActivity(WidgetIntents.openTab(context, "home"))
            )
        }
    }
}

class QuickDownloadWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickDownloadWidget()
}
