package jp.seo.uma.eventchecker.ui

import android.widget.ListView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import jp.seo.uma.eventchecker.core.GameEvent

/**
 * @author Seo-4d696b75
 * @version 2021/10/04.
 */

@BindingAdapter("gameEventTitle")
fun setEventTitle(view: TextView, event: GameEvent?) {
    view.text = event?.title ?: "None"
}

@BindingAdapter("gameEventChoice")
fun setEventChoiceList(view: ListView, event: GameEvent?) {
    view.adapter = event?.let {
        EventChoiceAdapter(view.context, it.choices)
    }
}
