package jp.seo.uma.eventchecker.ui.adapter

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import jp.seo.uma.eventchecker.data.model.GameEvent

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

@BindingAdapter("imageTintColor")
fun setImageTintColor(view: ImageView, color: Int) {
    view.imageTintList = ColorStateList.valueOf(color)
}
