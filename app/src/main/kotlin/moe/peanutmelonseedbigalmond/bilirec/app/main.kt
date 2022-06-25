package moe.peanutmelonseedbigalmond.bilirec.app

import kotlinx.coroutines.coroutineScope

suspend fun main(args: Array<String>) = coroutineScope {
    CommandProcessor(args).processAsync()
}
