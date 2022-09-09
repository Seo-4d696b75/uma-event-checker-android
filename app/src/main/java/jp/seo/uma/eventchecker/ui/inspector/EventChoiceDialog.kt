package jp.seo.uma.eventchecker.ui.inspector

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.DialogEventChoiceBinding
import jp.seo.uma.eventchecker.model.GameEvent
import jp.seo.uma.eventchecker.ui.overlay.EventChoiceViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class EventChoiceDialog : DialogFragment() {

    companion object {
        // TODO safeArgs
        fun getInstance(event: GameEvent) = EventChoiceDialog().apply {
            arguments = bundleOf(
                KEY_ARG_GAME_EVENT to event
            )
        }

        private const val KEY_ARG_GAME_EVENT = "game_event"
    }

    private val event: GameEvent by lazy {
        requireArguments().getSerializable(KEY_ARG_GAME_EVENT) as GameEvent
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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