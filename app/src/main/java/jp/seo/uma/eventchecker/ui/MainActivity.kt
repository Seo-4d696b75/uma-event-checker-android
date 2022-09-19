package jp.seo.uma.eventchecker.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.repository.AppEvent
import jp.seo.uma.eventchecker.ui.checker.CheckerService
import jp.seo.uma.eventchecker.ui.update.DataUpdateDialogDirections
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

/**
 * ユーザとのインタラクションが必要な処理
 *
 * - UI操作が必要なパーミッション・API使用の要求
 * - アプリ開始時に初期化完了まで待機させる
 * - サービスの開始・終了
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var projectionManager: MediaProjectionManager

    private val openCvCallback by lazy {
        object : BaseLoaderCallback(applicationContext) {
            override fun onManagerConnected(status: Int) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    // init data and etc.
                    viewModel.checkDataUpdate()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "fail to get OpenCV library",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private val mediaProjectionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val resultCode = it.resultCode
        val data = it.data
        if (resultCode == Activity.RESULT_OK && data != null) {
            val projection = projectionManager.getMediaProjection(resultCode, data)
            viewModel.startCapture(projection)
        } else {
            Toast.makeText(this, "fail to get capture", Toast.LENGTH_SHORT).show()
            stopService()
            finish()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            // retry to start service
            startService()
        } else {
            Toast.makeText(
                this,
                "overlay permission not granted",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // handle event
        viewModel.event
            .flowWithLifecycle(lifecycle)
            .onEach {
                Log.d("Event", it.toString())
                when (it) {
                    is AppEvent.Error -> {
                        Log.e("MainActivity", it.e.toString())
                        Toast.makeText(this, it.e.message, Toast.LENGTH_LONG).show()
                        stopService()
                        finish()
                    }
                    is AppEvent.DataUpdateRequest -> {
                        findNavController(R.id.nav_host_main).navigate(
                            DataUpdateDialogDirections.actionShowDataUpdateDialog(false, it.info)
                        )
                    }
                    is AppEvent.DataUpdateConfirm -> {
                        if (it.confirm) {
                            // run update via network
                            findNavController(R.id.nav_host_main).navigateWhenDialogClosed(
                                DataUpdateDialogDirections.actionShowDataUpdateDialog(
                                    true,
                                    it.info
                                ),
                                R.id.dialog_data_update,
                                lifecycle,
                            )
                        } else {
                            // load local storage
                            viewModel.loadData()
                        }
                    }
                    is AppEvent.StartChecker -> startService()
                    is AppEvent.StopChecker -> stopService()
                }
            }
            .launchIn(lifecycleScope)

        // check OpenCV and init ViewModel
        if (OpenCVLoader.initDebug()) {
            openCvCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            OpenCVLoader.initAsync("4.6.0", applicationContext, openCvCallback)
        }

    }

    private fun startService() {

        // check permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                applicationContext,
                "Need \"DrawOverlay\" Permission",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${packageName}")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        val intent = Intent(this, CheckerService::class.java)
        startForegroundService(intent)

        // init MediaProjection API
        projectionManager =
            getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionPermissionLauncher.launch(projectionManager.createScreenCaptureIntent())
        viewModel.setMetrics(windowManager)
    }

    private fun stopService() {
        viewModel.stopCapture()
        val intent = Intent(this, CheckerService::class.java)
        stopService(intent)
    }
}
