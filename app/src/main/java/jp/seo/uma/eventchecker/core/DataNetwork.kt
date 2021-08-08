package jp.seo.uma.eventchecker.core

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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

@ExperimentalSerializationApi
fun getDataNetwork(baseURL: String): DataNetwork {
    val client = OkHttpClient.Builder().build()
    val factory = Json { ignoreUnknownKeys = true }
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
) {

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
