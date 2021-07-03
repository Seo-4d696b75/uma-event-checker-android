package jp.seo.uma.eventchecker.core

import android.content.Context
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
