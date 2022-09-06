package jp.seo.uma.eventchecker.ui.overlay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.model.EventChoice
import jp.seo.uma.eventchecker.databinding.ListChoiceItemBinding

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
        val binding: ListChoiceItemBinding = if (convertView == null) {
            ListChoiceItemBinding.inflate(inflater).also {
                it.root.tag = it
            }
        } else {
            requireNotNull(DataBindingUtil.bind(convertView))
            // may be crash
            //ListChoiceItemBinding.bind(convertView)
        }
        getItem(position)?.let { choice ->
            binding.choice = choice
            binding.symbolColor = tintColors[position % tintColors.size]
            binding.executePendingBindings()
        }
        return binding.root
    }
}
