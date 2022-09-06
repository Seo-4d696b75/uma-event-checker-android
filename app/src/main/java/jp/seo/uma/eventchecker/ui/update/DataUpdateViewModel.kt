package jp.seo.uma.eventchecker.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.api.EventDataInfo
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.repository.AppRepository
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.ui.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataUpdateViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val dataRepository: DataRepository,
    private val imgProcess: ImageProcess,
) : ViewModel() {

    val progress = dataRepository
        .updateState
        .mapState(viewModelScope) {
            it?.progress ?: 0
        }

    val status = dataRepository
        .updateState
        .mapState(viewModelScope) {
            it?.state ?: ""
        }

    private val _result = MutableStateFlow<Boolean?>(null)
    val result: StateFlow<Boolean?> = _result

    fun confirmUpdateData(info: EventDataInfo, confirm: Boolean) = viewModelScope.launch {
        appRepository.confirmUpdateData(info, confirm)
    }

    fun runUpdateData(info: EventDataInfo) = viewModelScope.launch {
        try {
            dataRepository.updateData(info)
            imgProcess.init()
            _result.update { true }
        } catch (e: Exception) {
            appRepository.emitError(e)
            _result.update { false }
        }
    }
}