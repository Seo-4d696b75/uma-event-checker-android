package jp.seo.uma.eventchecker.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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
