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

/** Widget 5 — Platform shortcuts (Instagram / Facebook / YouTube). Opens Browser tab. */
class PlatformsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                PlatformsContent()
            }
        }
    }

    @Composable
    private fun PlatformsContent() {
        Column(
            modifier = androidx.glance.GlanceModifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Platforms",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
            Text(text = "1-tap open & download", style = TextStyle(fontSize = 12.sp))
            Spacer(modifier = androidx.glance.GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    text = "Browser",
                    onClick = WidgetIntents.openTabAction("browser")
                )
                Spacer(modifier = androidx.glance.GlanceModifier.width(8.dp))
                Button(
                    text = "Home",
                    onClick = WidgetIntents.openTabAction("home")
                )
            }
        }
    }
}

class PlatformsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlatformsWidget()
}
