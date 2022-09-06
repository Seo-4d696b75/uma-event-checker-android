package jp.seo.uma.eventchecker.img

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes
import okhttp3.ResponseBody
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun copyAssetsToFiles(context: Context, src: String, dst: File) {
    val manager = context.resources.assets
    manager.open(src).writeFile(dst)
}

fun ResponseBody.saveFile(dst: File) {
    this.byteStream().writeFile(dst)
}

fun InputStream.writeFile(dst: File) {
    this.use { reader ->
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

fun Resources.readFloat(@DimenRes id: Int): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.getFloat(id)
    } else {
        val value = TypedValue()
        this.getValue(id, value, true)
        value.float
    }
}

fun AssetManager.getBitmap(path: String): Bitmap {
    return BitmapFactory.decodeStream(open(path))
}

fun readBitmap(dir: File, path: String): Bitmap {
    val stream = File(dir, path).inputStream()
    return BitmapFactory.decodeStream(stream)
}

fun Bitmap.toMat(): Mat {
    val mat = Mat()
    Utils.bitmapToMat(this, mat)
    return mat
}

fun Bitmap.toGrayMat(): Mat {
    val mat = toMat()
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
    return mat
}

fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        width(), height(),
        Bitmap.Config.ARGB_8888
    )
    Utils.matToBitmap(this, bitmap)
    return bitmap
}
