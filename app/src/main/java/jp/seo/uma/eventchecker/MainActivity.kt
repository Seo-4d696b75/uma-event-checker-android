package jp.seo.uma.eventchecker

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dir = File(applicationContext.filesDir, OCR_DATA_DIR)
        if (!dir.exists() || !dir.isDirectory) {
            if (!dir.mkdir()) {
                throw RuntimeException("fail to mkdir: $dir")
            }
        }
        val file = File(dir, OCR_TRAINED_DATA)
        copyAssetsToFiles(applicationContext, OCR_TRAINED_DATA, file)
        val api = TessBaseAPI()
        val init = api.init(filesDir.toString(), "jpn")
        api.end()
    }

    private fun copyAssetsToFiles(context: Context, src: String, dst: File) {
        val manager = context.resources.assets
        manager.open(src).use { reader ->
            val buffer = ByteArray(1024)
            FileOutputStream(dst).use { writer ->
                while (true) {
                    val length = reader.read(buffer)
                    if (length < 0) break
                    writer.write(buffer, 0, length)
                }
                writer.flush()
            }
        }
    }
}
