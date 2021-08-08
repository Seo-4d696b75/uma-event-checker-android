package jp.seo.uma.eventchecker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.spyk
import jp.seo.uma.eventchecker.core.DataRepository
import jp.seo.uma.eventchecker.core.getDataNetwork
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Test

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
class DataRepositoryTest {

    private val repository: DataRepository

    init {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val network = getDataNetwork(context.getString(R.string.data_repository_base_url))
        repository = DataRepository(context, network)
    }

    @Test
    fun test_checkUpdate() {
        val mock = spyk(repository)
        every { mock.dataVersion } returns 0L
        val info = runBlocking {
            mock.checkUpdate()
        }
        assertThat(info, Matchers.notNullValue())
        every { mock getProperty "dataVersion" } returns info!!.version
        val info2 = runBlocking {
            mock.checkUpdate()
        }
        assertThat(info2, Matchers.nullValue())
    }

    @Test
    fun test_update() {
        val info = runBlocking { repository.checkUpdate() }
        assertThat(info, Matchers.notNullValue())
        runBlocking { repository.updateData(info!!) }
        val info2 = runBlocking { repository.checkUpdate() }
        assertThat(info2, Matchers.nullValue())
        runBlocking { repository.loadData() }
        assertThat(repository.dataVersion, Matchers.`is`(info!!.version))
    }
}
