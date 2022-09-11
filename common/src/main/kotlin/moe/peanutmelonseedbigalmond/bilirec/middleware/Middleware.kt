package moe.peanutmelonseedbigalmond.bilirec.middleware

fun interface Middleware<Data> {
    fun execute(context: MiddlewareContext<Data,*>, next: MiddlewareNext)
}