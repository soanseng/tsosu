package app.tsosu.ui.screens.weeklyreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.WeeklyReview
import app.tsosu.domain.usecase.GetWeeklyReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    getWeeklyReviewUseCase: GetWeeklyReviewUseCase,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    val review: StateFlow<WeeklyReview?> = getWeeklyReviewUseCase(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
