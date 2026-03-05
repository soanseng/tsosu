package app.tsosu.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.graphics.Color
import androidx.glance.layout.Spacer
import androidx.glance.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class FocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Focus 3",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ColorProvider(Color.BLACK),
                    ),
                )
                Spacer(GlanceModifier.size(8.dp))
                Text(
                    text = "Open Tsosu to set your focus tasks",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(Color.GRAY),
                    ),
                )
            }
        }
    }
}
