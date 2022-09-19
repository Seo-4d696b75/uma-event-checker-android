package jp.seo.uma.eventchecker.ui.inspector

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.DialogEventChoiceBinding
import jp.seo.uma.eventchecker.ui.overlay.EventChoiceViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class EventChoiceDialog : DialogFragment() {

    private val args: EventChoiceDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val event = args.event
        val binding = DialogEventChoiceBinding.inflate(
            layoutInflater
        ).also {
            it.includeEventChoice.listGameEvent.apply {
                divider = null
                dividerHeight = 0
            }
            it.viewModel = object : EventChoiceViewModel {
                override val currentEvent = MutableStateFlow(event)
            }
            it.lifecycleOwner = this
        }

        val context = requireContext()
        return AlertDialog.Builder(context).apply {
            setTitle(event.title)
            setView(binding.root)
            setPositiveButton(R.string.event_choice_dialog_close) { _, _ ->
                dismiss()
            }
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}