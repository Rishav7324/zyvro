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
import androidx.glance.Button
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Widget 2 — Queue progress shortcut (2x1).
 * Opens the Queue tab. Live % updates are pushed by
 * DownloadService via QueueWidgetUpdater (best-effort).
 */
class QueueProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QueueContent()
            }
        }
    }

    @Composable
    private fun QueueContent() {
        val context = LocalContext.current
        Column(
            modifier = androidx.glance.GlanceModifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Download Queue",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
            Text(text = "Live speed, ETA & progress", style = TextStyle(fontSize = 12.sp))
            Spacer(modifier = androidx.glance.GlanceModifier.height(8.dp))
            Button(
                text = "Open Queue",
                onClick = actionStartActivity(WidgetIntents.openTab(context, "queue"))
            )
        }
    }
}

class QueueProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QueueProgressWidget()
}
