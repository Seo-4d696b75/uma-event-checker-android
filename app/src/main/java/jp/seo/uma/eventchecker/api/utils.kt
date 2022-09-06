package jp.seo.uma.eventchecker.api

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio


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