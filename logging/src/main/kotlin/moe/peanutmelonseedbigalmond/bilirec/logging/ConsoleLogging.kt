package moe.peanutmelonseedbigalmond.bilirec.logging

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.ThreadContext

class ConsoleLogging(private val roomId: Long? = null, cls: Any? = null) : BaseLogging() {
    private var logger: Logger =
        if (cls == null) LogManager.getLogger(LogManager.ROOT_LOGGER_NAME) else LogManager.getLogger(cls::class.java)

    override fun trace(msg: String) {
        beforeLogging()
        logger.trace(msg)
        afterLogging()
    }

    override fun debug(msg: String) {
        beforeLogging()
        logger.debug(msg)
        afterLogging()
    }

    override fun info(msg: String) {
        beforeLogging()
        logger.info(msg)
        afterLogging()
    }

    override fun warn(msg: String) {
        beforeLogging()
        logger.warn(msg)
        afterLogging()
    }

    override fun error(msg: String) {
        beforeLogging()
        logger.error(msg)
        afterLogging()
    }

    private fun beforeLogging() {
        if (roomId != null) {
            ThreadContext.put("roomId", roomId.toString())
        }
    }

    private fun afterLogging() {
        ThreadContext.remove("roomId")
    }
}