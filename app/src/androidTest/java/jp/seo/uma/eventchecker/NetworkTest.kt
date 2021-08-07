package jp.seo.uma.eventchecker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.seo.uma.eventchecker.core.DataNetwork
import jp.seo.uma.eventchecker.core.getDataNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
@RunWith(AndroidJUnit4::class)
class NetworkTest {

    lateinit var client: DataNetwork

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        client = getDataNetwork(context.getString(R.string.data_repository_base_url))
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
        assertThat(data.owners.charaEventOwners.size, Matchers.greaterThan(0))
        assertThat(data.owners.supportEventOwners.size, Matchers.greaterThan(0))
    }
}
