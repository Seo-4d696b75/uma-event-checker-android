package jp.seo.uma.eventchecker.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun <T, R> StateFlow<T>.mapState(
    scope: CoroutineScope,
    convert: (T) -> R
) = map { convert(it) }.stateIn(
    scope,
    SharingStarted.Eagerly,
    convert(value),
)