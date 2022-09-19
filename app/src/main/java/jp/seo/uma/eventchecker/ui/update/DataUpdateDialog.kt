package jp.seo.uma.eventchecker.ui.update

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.DialogUpdateProgressBinding
import jp.seo.uma.eventchecker.databinding.DialogUpdateRequestBinding
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
@AndroidEntryPoint
class DataUpdateDialog : DialogFragment() {

    private val viewModel: DataUpdateViewModel by viewModels()

    private val args: DataUpdateDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val runUpdate = args.runUpdate
        val info = args.info
        if (!runUpdate) {
            val binding = DialogUpdateRequestBinding.inflate(layoutInflater)
            binding.info = info
            builder.setTitle(R.string.data_update_title)
            builder.setPositiveButton(R.string.data_update_dialog_button_positive) { _, _ ->
                viewModel.confirmUpdateData(info, true)
                dismiss()
            }
            builder.setNegativeButton(R.string.data_update_dialog_button_negative) { _, _ ->
                viewModel.confirmUpdateData(info, false)
                dismiss()
            }
            builder.setView(binding.root)
        } else {
            val binding = DialogUpdateProgressBinding.inflate(layoutInflater)
            binding.viewModel = viewModel
            binding.lifecycleOwner = this
            builder.setTitle(R.string.data_update_title)
            builder.setView(binding.root)
            // run data update
            viewModel.runUpdateData(info)
            // when update done, close dialog
            viewModel.result
                .flowWithLifecycle(lifecycle)
                .filterNotNull()
                .onEach { dismiss() }
                .launchIn(lifecycleScope)
        }

        return builder.create().also {
            it.setCanceledOnTouchOutside(false)
        }
    }
}
