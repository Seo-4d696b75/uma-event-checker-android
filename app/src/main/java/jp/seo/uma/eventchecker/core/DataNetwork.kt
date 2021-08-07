package jp.seo.uma.eventchecker.core

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
interface DataNetwork {
    @GET("info.json")
    suspend fun getDataInfo(): EventDataInfo

    @GET("data.json")
    suspend fun getData(): GameEventData
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
)
