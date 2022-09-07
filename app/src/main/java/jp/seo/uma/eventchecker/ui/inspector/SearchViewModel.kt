package jp.seo.uma.eventchecker.ui.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.ui.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val imageProcess: ImageProcess,
    private val dataRepository: DataRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun setQueryAsDetectedString() {
        _query.update { imageProcess.title.value ?: "" }
    }

    fun setQuery(value: String) {
        _query.update { value }
    }

    val events = _query.mapState(viewModelScope) {
        dataRepository.searchForEvent(it, 10)
    }
}