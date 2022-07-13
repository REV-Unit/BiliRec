package moe.peanutmelonseedbigalmond.bilirec.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext

// https://github.com/Kotlin/kotlinx.coroutines/issues/1686#issuecomment-777357672
// https://gist.github.com/elizarov/9a48b9709ffd508909d34fab6786acfe
data class ReentrantMutexContextKey(
    val mutex: Mutex
) : CoroutineContext.Key<ReentrantMutexContextElement>