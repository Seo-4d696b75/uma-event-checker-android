package jp.seo.uma.eventchecker.ui

import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.CheckerService
import jp.seo.uma.eventchecker.core.MainViewModel
import jp.seo.uma.eventchecker.core.ScreenCapture
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAPTURE = 33333
        const val REQUEST_OVERLAY = 44444
    }

    @Inject
    lateinit var store: ViewModelStore

    @Inject
    lateinit var capture: ScreenCapture

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(store)
    }

    private lateinit var projectionManager: MediaProjectionManager

    private val openCvCallback by lazy {
        object : BaseLoaderCallback(applicationContext) {
            override fun onManagerConnected(status: Int) {
                if (status == LoaderCallbackInterface.SUCCESS) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progress = findViewById<View>(R.id.progres_main)

        viewModel.loading.observe(this) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
        }

        // check OpenCV and init ViewModel
        if (OpenCVLoader.initDebug()) {
            openCvCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            OpenCVLoader.initAsync("4.5.2", applicationContext, openCvCallback)
        }

    }

    override fun onResume() {
        super.onResume()
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
            startActivityForResult(intent, REQUEST_OVERLAY)
            return
        }
        // init MediaProjection API
        if (!capture.initialized) {
            projectionManager =
                getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_CAPTURE
            )
            capture.setMetrics(windowManager)
            return
        }
        val intent = Intent(this, CheckerService::class.java)
        startForegroundService(intent)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val projection = projectionManager.getMediaProjection(resultCode, data)
                capture.setMediaProjection(projection)
            } else {
                Toast.makeText(this, "fail to get capture", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == REQUEST_OVERLAY) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "overlay permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}
