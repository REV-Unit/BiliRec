package moe.peanutmelonseedbigalmond.bilirec.coroutine

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// https://github.com/Kotlin/kotlinx.coroutines/issues/1686#issuecomment-777357672
// https://gist.github.com/elizarov/9a48b9709ffd508909d34fab6786acfe
@OptIn(ExperimentalContracts::class)
suspend fun <T> Mutex.withReentrantLock(block: suspend () -> T): T {
    contract {
        callsInPlace(block,InvocationKind.EXACTLY_ONCE)
    }
    val key = ReentrantMutexContextKey(this)
    // call block directly when this mutex is already locked in the context
    if (coroutineContext[key] != null) return block()
    // otherwise add it to the context and lock the mutex
    return withContext(ReentrantMutexContextElement(key)) {
        withLock { block() }
    }
}
