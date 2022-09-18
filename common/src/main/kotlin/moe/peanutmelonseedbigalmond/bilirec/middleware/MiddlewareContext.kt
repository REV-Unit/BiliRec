package moe.peanutmelonseedbigalmond.bilirec.middleware

import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory

class MiddlewareContext<Data, Extra>(val data: Data, val extra: Extra) {
    var logger = LoggingFactory.getLogger(obj = this)
}