package jp.seo.uma.eventchecker.ui.inspector

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.ImageProcess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val imageProcess: ImageProcess,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun setQueryAsDetectedString() {
        _query.update { imageProcess.title.value ?: "" }
    }

    fun setQuery(value: String) {
        _query.update { value }
    }
}