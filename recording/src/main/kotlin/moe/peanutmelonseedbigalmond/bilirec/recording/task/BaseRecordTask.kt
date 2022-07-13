package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room

abstract class BaseRecordTask(protected val room: Room) : SuspendableCloseable {
    abstract val closed: Boolean
    protected open val logger = LoggingFactory.getLogger(room.roomConfig.roomId, this)
    abstract suspend fun prepare()
    abstract suspend fun start(baseFileName: String)

    // 结束录制，但是不结束任务
    abstract suspend fun stopRecording()
}