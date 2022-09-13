package jp.seo.uma.eventchecker

import jp.seo.uma.eventchecker.api.DataNetwork
import jp.seo.uma.eventchecker.api.getDataNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Retrofitのテスト
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
class NetworkUnitTest {

    // TODO change to main URL
    private val baseURL = "https://raw.githubusercontent.com/Seo-4d696b75/uma-event-data/feature/update-format/"

    lateinit var client: DataNetwork

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setup() {
        client = getDataNetwork(baseURL) {}
    }

    @Test
    fun getDataInfo() = runBlocking(Dispatchers.IO) {
        val info = client.getDataInfo()
        val d = Calendar.getInstance()
        val v =
            d.get(Calendar.YEAR) * 10000L + (d.get(Calendar.MONTH) + 1) * 100L + d.get(Calendar.DAY_OF_MONTH)
        assertThat(info.version, Matchers.lessThanOrEqualTo(v))
        assertThat(info.size, Matchers.greaterThan(0L))
    }

    @Test
    fun getData() = runBlocking(Dispatchers.IO) {
        val data = client.getData()
        assertThat(data.events.size, Matchers.greaterThan(0))
        assertThat(data.owners.partners.size, Matchers.greaterThan(0))
        assertThat(data.owners.supportCards.size, Matchers.greaterThan(0))
    }
}
