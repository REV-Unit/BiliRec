package moe.peanutmelonseedbigalmond.bilirec

import moe.peanutmelonseedbigalmond.bilirec.interfaces.AsyncCloseable
import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalContracts::class)
suspend inline fun <T : AsyncCloseable?, R> T.use(
    context: CoroutineContext = EmptyCoroutineContext,
    action: (T) -> R
): R {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return action(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {}
            exception == null -> closeAsync()
            else -> this.closeQuietlyAsync()
        }
    }
}

suspend fun AsyncCloseable?.closeQuietlyAsync() {
    try {
        this?.closeAsync()
    } catch (_: Throwable) {
    }
}