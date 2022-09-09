package jp.seo.uma.eventchecker.ui.overlay

import jp.seo.uma.eventchecker.model.GameEvent
import kotlinx.coroutines.flow.StateFlow

interface EventChoiceViewModel {
    val currentEvent: StateFlow<GameEvent?>
}