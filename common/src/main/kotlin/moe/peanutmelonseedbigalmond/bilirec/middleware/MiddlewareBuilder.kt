package moe.peanutmelonseedbigalmond.bilirec.middleware

class MiddlewareBuilder<Data> {
    private val middlewareList = mutableListOf<Middleware<Data>>()

    fun use(middleware: Middleware<Data>): MiddlewareBuilder<Data> {
        this.middlewareList.add(middleware)
        return this
    }

    fun build(): (MiddlewareContext<Data, *>) -> Unit {
        middlewareList.reverse()
        var pipeline: MiddlewarePipeline<Data>? = null
        for (middleware in middlewareList) {
            pipeline = pipeline?.addHandler(middleware) ?: MiddlewarePipeline(middleware)
        }
        return pipeline!!::execute
    }

    private class MiddlewarePipeline<Data>(private val currentHandler: Middleware<Data>) {
        fun addHandler(newHandler: Middleware<Data>): MiddlewarePipeline<Data> {
            return MiddlewarePipeline { context, next ->
                val next2 = MiddlewareNext { currentHandler.execute(context, next) }
                newHandler.execute(context, next2)
            }
        }

        fun execute(context: MiddlewareContext<Data, *>) {
            this.currentHandler.execute(context) {}
        }
    }
}