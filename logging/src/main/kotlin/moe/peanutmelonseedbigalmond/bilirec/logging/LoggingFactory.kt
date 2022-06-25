package moe.peanutmelonseedbigalmond.bilirec.logging

object LoggingFactory {
    fun getLogger(roomId: Long? = null, obj: Any? = null): BaseLogging = ConsoleLogging(roomId, obj)
}