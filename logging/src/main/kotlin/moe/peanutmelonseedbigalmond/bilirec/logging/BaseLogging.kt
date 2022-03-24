package moe.peanutmelonseedbigalmond.bilirec.logging

abstract class BaseLogging {
    abstract fun trace(msg: String)
    abstract fun debug(msg: String)
    abstract fun info(msg: String)
    abstract fun warn(msg: String)
    abstract fun error(msg: String)
}