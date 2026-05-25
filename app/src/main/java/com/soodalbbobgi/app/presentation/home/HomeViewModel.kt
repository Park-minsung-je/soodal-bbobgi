package com.soodalbbobgi.app.presentation.home

import androidx.lifecycle.ViewModel
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class RecentItem(val name: String, val kind: String, val grade: Grade)

data class HomeUiState(
    val nickname: String = "Soodal",
    val shells: Int = 34,
    val pearls: Int = 12,
    val totalDistance: Int = 12540,
    val swimSessions: Int = 18,
    val totalKcal: Int = 4280,
    val todayHasRecord: Boolean = false,
    val syncing: Boolean = false,
    val recentItems: List<RecentItem> = listOf(
        RecentItem("진주 수달", "char", Grade.SR),
        RecentItem("시안 라인", "frame", Grade.R),
        RecentItem("오로라", "bg", Grade.SR),
        RecentItem("수달이", "char", Grade.N),
        RecentItem("코랄", "char", Grade.R),
        RecentItem("버블", "bg", Grade.R),
        RecentItem("한밤", "bg", Grade.N),
    ),
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun onSync() {
        _uiState.value = _uiState.value.copy(syncing = true)
        _uiState.value = _uiState.value.copy(syncing = false, todayHasRecord = true, shells = _uiState.value.shells + 2)
    }
}
