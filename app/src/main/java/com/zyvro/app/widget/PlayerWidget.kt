package com.zyvro.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.Button
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Widget 4 — Mini player shortcut. Opens app; transport lives in-app + notification. */
class PlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                PlayerContent()
            }
        }
    }

    @Composable
    private fun PlayerContent() {
        Column(
            modifier = androidx.glance.GlanceModifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Now Playing",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
            Text(text = "Continue audio / video", style = TextStyle(fontSize = 12.sp))
            Spacer(modifier = androidx.glance.GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    text = "Player",
                    onClick = WidgetIntents.openTabAction("library")
                )
                Spacer(modifier = androidx.glance.GlanceModifier.width(8.dp))
                Button(
                    text = "Queue",
                    onClick = WidgetIntents.openTabAction("queue")
                )
            }
        }
    }
}

class PlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerWidget()
}
