package jp.seo.uma.eventchecker.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OverlayViewModel @Inject constructor(
    data: DataRepository,
    setting: SettingRepository,
) : ViewModel() {

    val show = combine(
        data.currentEvent.asFlow(),
        setting.isDebugDialogShown,
    ) { event, show ->
        event != null && !show
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false,
    )

    val currentEvent = data.currentEvent.asFlow().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )
}