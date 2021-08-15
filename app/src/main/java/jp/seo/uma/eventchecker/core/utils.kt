package jp.seo.uma.eventchecker.core

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.CoroutineContext


/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */

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

/**
 * [@see](https://qiita.com/KazaKago/items/acce0c1a970441b44f39)
 */
class LiveEvent<T> : LiveData<T>() {

    private val dispatched = mutableSetOf<String>()

    fun observe(owner: LifecycleOwner, tag: String, observer: Observer<in T>) {
        super.observe(owner) {
            if (dispatched.add(tag)) observer.onChanged(it)
        }
    }

    @Deprecated(
        message = "Multiple observers registered but only one will be notified of changes. set tags for each observer.",
        replaceWith = ReplaceWith("observe(owner, \"tag\", observer)"),
        level = DeprecationLevel.HIDDEN
    )
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, observer)
    }

    fun observeForever(tag: String, observer: Observer<in T>) {
        super.observeForever {
            if (dispatched.add(tag)) observer.onChanged(it)
        }
    }

    @Deprecated(
        message = "Multiple observers registered but only one will be notified of changes. set tags for each observer.",
        replaceWith = ReplaceWith("observeForever(\"tag\", observer)"),
        level = DeprecationLevel.HIDDEN
    )
    override fun observeForever(observer: Observer<in T>) {
        super.observeForever(observer)
    }

    fun call(value: T) {
        setValue(value)
    }

    fun postCall(value: T) {
        postValue(value)
    }

    override fun setValue(value: T) {
        dispatched.clear()
        super.setValue(value)
    }
}

// https://stackoverflow.com/questions/42118924/android-retrofit-download-progress
class ProgressResponseBody(
    private val adapt: ResponseBody,
    private val listener: (Long) -> Unit,
) : ResponseBody() {

    private var buf: BufferedSource? = null

    override fun contentType(): MediaType? = adapt.contentType()

    override fun contentLength(): Long = adapt.contentLength()

    override fun source(): BufferedSource {
        return buf ?: kotlin.run {
            val s = object : ForwardingSource(adapt.source()) {
                private var totalBytes: Long = 0L
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val readBytes = super.read(sink, byteCount)
                    if (readBytes > 0L) totalBytes += readBytes
                    listener(totalBytes)
                    return readBytes
                }
            }
            val b = Okio.buffer(s)
            buf = b
            b
        }
    }

}

/**
 * ダウンロード中の進捗状況を確認するコールバックを登録する.
 *
 * @param listener
 */
fun OkHttpClient.Builder.addProgressCallback(listener: ((bytes: Long) -> Unit)): OkHttpClient.Builder {
    this.addInterceptor { chain ->
        val res = chain.proceed(chain.request())
        val body = res.body()
        body?.let {
            res.newBuilder()
                .body(ProgressResponseBody(it, listener))
                .build()
        } ?: res
    }
    return this
}

/**
 * Performs the given [process] on each element in parallel.
 *
 * The given numbers [coroutineCount] of coroutines will process each element
 * in order of [iterator].
 *
 * @param process any action to be done on each element
 * @param onProcessed callback invoked every time each action has been completed,
 *  this call is synchronized.
 * @param coroutineCount number of coroutines which will perform actions
 * @param context in which context the coroutines will run
 */
suspend fun <E> Iterable<E>.forEachParallel(
    process: suspend ((element: E) -> Unit),
    onProcessed: ((element: E, cnt: Int) -> Unit)? = null,
    coroutineCount: Int = 4,
    context: CoroutineContext = Dispatchers.Default
) = withContext(context) {
    val mutex = Mutex()
    val itr = iterator()
    var cnt = 0
    val coroutines = Array(coroutineCount) {
        async {
            while (true) {
                val next = mutex.withLock {
                    if (itr.hasNext()) itr.next() else null
                } ?: break
                process.invoke(next)
                onProcessed?.let { callback ->
                    mutex.withLock {
                        callback.invoke(next, ++cnt)
                    }
                }
            }
        }
    }
    coroutines.forEach { it.await() }
}

suspend fun <E, R> Array<E>.mapParallel(
    process: suspend ((element: E) -> R),
    onProcessed: ((result: R, cnt: Int) -> Unit)? = null,
    coroutineCount: Int = 4,
    context: CoroutineContext = Dispatchers.Default
): List<R> {
    val result = MutableList<R?>(this.size) { null }
    this.indices.forEachParallel(
        process = { idx ->
            val e = this[idx]
            val r = process.invoke(e)
            result[idx] = r
        },
        onProcessed = { idx, cnt ->
            onProcessed?.invoke(result[idx]!!, cnt)
        },
        coroutineCount, context
    )
    return result.map { it!! }
}
