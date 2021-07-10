package jp.seo.uma.eventchecker.view

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import jp.seo.uma.eventchecker.R

import jp.seo.uma.eventchecker.core.EventChoice

/**
 * @author Seo-4d696b75
 * @version 2021/07/06.
 */
class EventChoiceAdapter(
    context: Context,
    data: Array<EventChoice>
) : ArrayAdapter<EventChoice>(context, 0, data) {

    private val inflater = LayoutInflater.from(context)
    private val tintColors = context.resources.getIntArray(R.array.uma_symbol_tint)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: run {
            inflater.inflate(R.layout.list_choice_item, parent, false)
        }
        getItem(position)?.let { choice ->
            view.findViewById<TextView>(R.id.text_choice_name).text = choice.name
            view.findViewById<TextView>(R.id.text_choice_details).text = choice.formatMessage()
            view.findViewById<ImageView>(R.id.img_choice_symbol).let {
                val idx = position % tintColors.size
                it.imageTintList = ColorStateList.valueOf(tintColors[idx])
            }
        }
        return view
    }
}
