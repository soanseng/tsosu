package app.tsosu.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberHaptic(): HapticHelper {
    val view = LocalView.current
    return HapticHelper(view)
}

class HapticHelper(private val view: View) {
    fun confirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    fun reject() = view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    fun longPress() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    fun gestureStart() = view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
}
