package jp.seo.uma.eventchecker.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.core.CheckerService
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var store: ViewModelStore

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(store)
    }

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
        viewModel.bitmap.observe(this){
            image.setImageBitmap(it)
        }

    }
}
