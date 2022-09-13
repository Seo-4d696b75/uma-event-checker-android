package jp.seo.uma.eventchecker

import jp.seo.uma.eventchecker.api.getDataNetwork
import jp.seo.uma.eventchecker.model.GameEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.lucene.search.spell.LevensteinDistance
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * @author Seo-4d696b75
 * @version 2021/08/19.
 */
class EventSearchUnitTest {

    // TODO change to main URL
    private val baseURL = "https://raw.githubusercontent.com/Seo-4d696b75/uma-event-data/feature/update-format/"

    private lateinit var data: Array<GameEvent>

    private val seed = 20210819L

    @Before
    fun setup() {
        val client = getDataNetwork(baseURL) {}
        data = runBlocking { client.getData().events }
        println("event data size: ${data.size}")
    }

    @Test
    fun search() {
        val rand = Random(seed)
        val algo = LevensteinDistance()
        var time = 0L
        (0 until 1000).forEach {
            val target = data[rand.nextInt(data.size)]
            val start = System.nanoTime()
            val score = data.map { algo.getDistance(it.title, target.title) }
            time += (System.nanoTime() - start)
            val max = score.maxOrNull()
            assertThat(max, Matchers.notNullValue())
            assertThat(max, Matchers.`is`(1.0f))
            val list = data.filterIndexed { index, gameEvent -> score[index] == 1f }
            assertThat(list, Matchers.hasItem(target))
        }
        println("single > time: ${time / 1000f / 1000000}ms")
    }


    @Test
    fun search_parallel() {
        val rand = Random(seed)
        val algo = LevensteinDistance()
        var time = 0L
        (0 until 1000).forEach {
            val target = data[rand.nextInt(data.size)]
            val start = System.nanoTime()
            val score = runBlocking {
                data.mapParallel { algo.getDistance(it.title, target.title) }
            }
            time += (System.nanoTime() - start)
            val max = score.maxOrNull()
            assertThat(max, Matchers.notNullValue())
            assertThat(max, Matchers.`is`(1.0f))
            val list = data.filterIndexed { index, gameEvent -> score[index] == 1f }
            assertThat(list, Matchers.hasItem(target))
        }
        println("parallel > time: ${time / 1000f / 1000000}ms")
    }

    suspend fun <E, R> Array<E>.mapParallel(action: ((E) -> R)): List<R> {
        val result = MutableList<R?>(this.size) { null }
        this.mapParallel(0, this.size, result, action)
        return result.map { it!! }
    }

    private suspend fun <E, R> Array<E>.mapParallel(
        from: Int,
        to: Int,
        result: MutableList<R?>,
        action: ((E) -> R)
    ): Unit = withContext(Dispatchers.Default) {
        if (from + 64 < to) {
            val mid = from + (to - from) / 2
            val left = async {
                mapParallel(from, mid, result, action)
            }
            val right = async {
                mapParallel(mid, to, result, action)
            }
            left.await()
            right.await()
        } else {
            for (i in from until to) {
                result[i] = action.invoke(get(i))
            }
        }
    }

}
