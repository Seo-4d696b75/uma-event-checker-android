package jp.seo.uma.eventchecker.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.api.EventDataInfo
import jp.seo.uma.eventchecker.core.MainViewModel
import jp.seo.uma.eventchecker.databinding.DialogUpdateProgressBinding
import jp.seo.uma.eventchecker.databinding.DialogUpdateRequestBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
@AndroidEntryPoint
class DataUpdateDialog : DialogFragment() {

    companion object {
        const val KEY_ARG_DATA_INFO = "data_info"

        fun getRequestDialog(info: EventDataInfo): DataUpdateDialog = DataUpdateDialog().also {
            val args = Bundle()
            args.putString(KEY_ARG_DATA_INFO, Json.encodeToString(info))
            it.arguments = args
        }

        fun getProgressDialog() = DataUpdateDialog()
    }

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        if (arguments?.containsKey(KEY_ARG_DATA_INFO) == true) {
            val binding = DialogUpdateRequestBinding.inflate(LayoutInflater.from(requireContext()))
            val info = Json.decodeFromString<EventDataInfo>(
                arguments?.getString(KEY_ARG_DATA_INFO)
                    ?: throw NullPointerException("info arg not found")
            )
            binding.info = info
            builder.setTitle(R.string.data_update_title)
            builder.setPositiveButton(R.string.data_update_dialog_button_positive) { dialog, which ->
                dialog.dismiss()
                viewModel.updateData(info)
            }
            builder.setNegativeButton(R.string.data_update_dialog_button_negative) { dialog, which ->
                dialog.dismiss()
                viewModel.loadData()
            }

            builder.setView(binding.root)
        } else {
            val binding = DialogUpdateProgressBinding.inflate(LayoutInflater.from(requireContext()))
            binding.viewModel = viewModel
            binding.lifecycleOwner = this
            builder.setTitle(R.string.data_update_title)
            builder.setView(binding.root)
            viewModel.update.observe(this, "update-progress-dialog") {
                if (it is MainViewModel.DataUpdateEvent.Complete) {
                    dismiss()
                }
            }
        }

        return builder.create().also {
            it.setCanceledOnTouchOutside(false)
        }
    }
}
