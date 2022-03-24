package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room

abstract class BaseRecordTask(room: Room) : AutoCloseable {
    abstract val closed:Boolean
    protected open val logger = LoggingFactory.getLogger(room.roomConfig.roomId, this)
    abstract fun start()
}