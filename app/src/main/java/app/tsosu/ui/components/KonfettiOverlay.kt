package app.tsosu.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun KonfettiOverlay(trigger: MutableIntState) {
    val count = trigger.intValue
    if (count == 0) return

    var lastSeen by remember { mutableIntStateOf(0) }
    if (count == lastSeen) return
    lastSeen = count

    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val colors = listOf(
        primary,
        Color(0xFFFFA726).toArgb(),
        Color(0xFFFFD54F).toArgb(),
        Color(0xFFFF8A65).toArgb(),
        Color(0xFF81C784).toArgb(),
        Color(0xFF4FC3F7).toArgb(),
        Color(0xFFBA68C8).toArgb(),
        Color(0xFFFF7043).toArgb(),
        Color(0xFFE0E0E0).toArgb(),
    )

    val parties = listOf(
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(0.5, 1.0),
            spread = 360,
        ),
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(0.0, 1.0),
            angle = 45,
            spread = 90,
        ),
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(1.0, 1.0),
            angle = 135,
            spread = 90,
        ),
    )

    KonfettiView(
        parties = parties,
        updateListener = object : OnParticleSystemUpdateListener {
            override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                // no-op: counter-based, no need to reset
            }
        },
    )
}
