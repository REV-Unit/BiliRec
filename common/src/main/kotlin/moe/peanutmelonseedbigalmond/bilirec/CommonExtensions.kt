package moe.peanutmelonseedbigalmond.bilirec

import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalContracts::class)
suspend inline fun <T : SuspendableCloseable?, R> T.use(
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
            exception == null -> close()
            else -> this.closeQuietlyAsync()
        }
    }
}

suspend fun SuspendableCloseable?.closeQuietlyAsync() {
    try {
        this?.close()
    } catch (_: Throwable) {
    }
}