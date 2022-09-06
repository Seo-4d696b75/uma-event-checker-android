package jp.seo.uma.eventchecker.api

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.model.GameEventData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.StringCharacterIterator
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
interface DataNetwork {
    @GET("info.json")
    suspend fun getDataInfo(): EventDataInfo

    @GET("data.json")
    suspend fun getData(): GameEventData

    @GET("icon/{file_name}")
    suspend fun getIconImage(@Path("file_name") name: String): ResponseBody
}

@OptIn(ExperimentalSerializationApi::class)
class NetworkClient @Inject constructor(
    @ApplicationContext context: Context
) {
    private val client = getDataNetwork(context.getString(R.string.data_repository_base_url)) {
        callback?.invoke(it)
    }

    private var callback: ((Long) -> Unit)? = null

    suspend fun getDataInfo() = client.getDataInfo()
    suspend fun getData() = client.getData()
    suspend fun getIconImage(name: String) = client.getIconImage(name)

    suspend fun getData(progress: ((Long) -> Unit)): GameEventData {
        callback = progress
        val result = kotlin.runCatching { client.getData() }
        callback = null
        return result.getOrThrow()
    }
}

private val json = Json { ignoreUnknownKeys = true }

@ExperimentalSerializationApi
fun getDataNetwork(baseURL: String, progress: ((Long) -> Unit)): DataNetwork {
    val client = OkHttpClient.Builder()
        .addProgressCallback(progress)
        .build()
    val factory = json
        .asConverterFactory(MediaType.get("application/json"))
    return Retrofit.Builder()
        .baseUrl(baseURL)
        .addConverterFactory(factory)
        .client(client)
        .build()
        .create(DataNetwork::class.java)
}

@Serializable
data class EventDataInfo(
    @SerialName("version")
    val version: Long,
    @SerialName("size")
    val size: Long
) : java.io.Serializable {

    fun fileSize(): String {
        var bytes = size
        if (bytes < 0) return "0 B"
        if (bytes < 1000) return "$bytes B"
        val ci = StringCharacterIterator("KMGTPE")
        while (bytes >= 999_950) {
            bytes /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", bytes.toFloat() / 1000.0f, ci.current())
    }
}
