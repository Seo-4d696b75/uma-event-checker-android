package jp.seo.uma.eventchecker.view

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.viewmodel.MainViewModel
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
                    // init img process
                    viewModel.init(applicationContext)
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
            // all ready. start service
            val intent = Intent(this, CheckerService::class.java)
            startForegroundService(intent)
            val projection = projectionManager.getMediaProjection(resultCode, data)
            viewModel.startCapture(projection)
        } else {
            Toast.makeText(this, "fail to get capture", Toast.LENGTH_SHORT).show()
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

        val progress = findViewById<View>(R.id.progress_main)
        val message = findViewById<TextView>(R.id.text_main)
        val button = findViewById<Button>(R.id.button_start)

        viewModel.loading.observe(this) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
            button.isEnabled = !it
        }

        viewModel.runningCapture.observe(this) {
            message.text =
                getString(if (it) R.string.message_main_running else R.string.message_main_idle)
            button.text = getString(if (it) R.string.button_stop else R.string.button_start)
        }

        button.setOnClickListener {
            when (viewModel.runningCapture.value) {
                true -> stopService()
                else -> startService()
            }
        }

        // check OpenCV and init ViewModel
        if (OpenCVLoader.initDebug()) {
            openCvCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            OpenCVLoader.initAsync("4.5.2", applicationContext, openCvCallback)
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
