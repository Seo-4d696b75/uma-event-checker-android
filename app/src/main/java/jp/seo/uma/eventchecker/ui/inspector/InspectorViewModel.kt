package jp.seo.uma.eventchecker.ui.inspector

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class InspectorViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
) : ViewModel() {

    private val _dismiss = MutableStateFlow(false)
    val dismiss: StateFlow<Boolean> = _dismiss

    fun onDismiss() {
        settingRepository.isDebugDialogShown.update { false }
        _dismiss.update { true }
    }
}