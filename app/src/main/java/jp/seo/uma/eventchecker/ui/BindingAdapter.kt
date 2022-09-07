package jp.seo.uma.eventchecker.ui

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.img.EventType
import jp.seo.uma.eventchecker.model.GameEvent
import jp.seo.uma.eventchecker.ui.overlay.EventChoiceAdapter

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

@BindingAdapter("srcBitmap")
fun setImageBitmap(view: ImageView, img: Bitmap?) {
    if (img == null) {
        view.setImageResource(R.drawable.ic_image_not_supported)
    } else {
        view.setImageBitmap(img)
    }
}

@BindingAdapter("eventType")
fun setEventType(view: TextView, type: EventType?) {
    view.text = type?.name ?: "none"
}

@BindingAdapter("searchScore")
fun setScore(view: TextView, score: Float?) {
    view.text = score?.let {
        String.format("%.4f", it)
    } ?: ""
}
