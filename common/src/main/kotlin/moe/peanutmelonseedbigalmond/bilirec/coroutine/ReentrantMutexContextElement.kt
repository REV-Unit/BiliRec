package moe.peanutmelonseedbigalmond.bilirec.coroutine

import kotlin.coroutines.CoroutineContext

// https://github.com/Kotlin/kotlinx.coroutines/issues/1686#issuecomment-777357672
// https://gist.github.com/elizarov/9a48b9709ffd508909d34fab6786acfe
class ReentrantMutexContextElement(
    override val key: ReentrantMutexContextKey
) : CoroutineContext.Element