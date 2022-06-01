package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import java.io.Closeable

abstract class BaseRecordTask(room: Room) : Closeable {
    abstract val closed:Boolean
    protected open val logger = LoggingFactory.getLogger(room.roomConfig.roomId, this)
    abstract fun prepare()
    abstract fun startAsync(baseFileName:String)
    // 结束录制，但是不结束任务
    abstract fun stopRecording()
}