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

/** Widget 3 — Storage / stats shortcut. Opens Library tab. */
class StatsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                StatsContent()
            }
        }
    }

    @Composable
    private fun StatsContent() {
        val context = LocalContext.current
        Column(
            modifier = androidx.glance.GlanceModifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Library & Storage",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
            Text(text = "Videos · Audio · Free space", style = TextStyle(fontSize = 12.sp))
            Spacer(modifier = androidx.glance.GlanceModifier.height(8.dp))
            Button(
                text = "Open Library",
                onClick = actionStartActivity(WidgetIntents.openTab(context, "library"))
            )
        }
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
