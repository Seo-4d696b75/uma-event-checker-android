package jp.seo.uma.eventchecker.ui

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.core.CheckerService
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.MainViewModel
import jp.seo.uma.eventchecker.core.ScreenCapture
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAPTURE = 33333
    }

    @Inject
    lateinit var store: ViewModelStore

    @Inject
    lateinit var capture: ScreenCapture

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(store)
    }

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(this, CheckerService::class.java)
        startForegroundService(intent)

        val image = findViewById<ImageView>(R.id.image_ocr)
        val ocrText = findViewById<TextView>(R.id.text_ocr)
        val progress = findViewById<View>(R.id.progres_main)

        viewModel.init(this)

        viewModel.ocrText.observe(this) {
            ocrText.text = it
        }
        viewModel.loading.observe(this) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.bitmap.observe(this) {
            image.setImageBitmap(it)
        }

        if (!capture.initialized) {
            projectionManager =
                getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_CAPTURE
            )
            capture.setMetrics(windowManager)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val projection = projectionManager.getMediaProjection(resultCode, data)
                capture.setMediaProjection(projection)
            }
        } else {
            Toast.makeText(this, "fail to get capture", Toast.LENGTH_LONG).show()
        }
    }
}
