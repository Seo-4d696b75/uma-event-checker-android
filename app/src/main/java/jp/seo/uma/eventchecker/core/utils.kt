package jp.seo.uma.eventchecker.core

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */

fun copyAssetsToFiles(context: Context, src: String, dst: File) {
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

fun AssetManager.getBitmap(path: String): Bitmap {
    return BitmapFactory.decodeStream(open(path))
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

fun Resources.readFloat(@DimenRes id: Int): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.getFloat(id)
    } else {
        val value = TypedValue()
        this.getValue(id, value, true)
        value.float
    }
}

private val PATTERN_REMOVAL = Regex("[\\p{P}\\p{S}\\s]")

/**
 * 文字列の比較検索のために正規化
 *
 * - OCRによる識別精度が低い記号類の無視
 * - 数字の半角・全角の統一
 */
fun String.normalizeForComparison(): String {
    return this.replace('０', '９', '0') // 数字は半角に統一
        .replace('①', '⑨', '1') // Tesseractの数字
        .replace(PATTERN_REMOVAL, "") // 空白・記号などは無視
}

private fun String.replace(start: Char, end: Char, replaceStart: Char): String {
    return this.toCharArray().map { c ->
        if (c in (start..end)) {
            replaceStart + (c - start)
        } else c
    }.joinToString()
}
