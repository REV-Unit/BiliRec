package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.RawRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.StrandRecordTask
import kotlin.coroutines.CoroutineContext

object RecordTaskFactory {
    fun getRecordTask(room: Room, coroutineContext: CoroutineContext): BaseRecordTask =
        when (room.roomConfig.recordMode) {
            RoomConfig.RecordMode.RAW -> RawRecordTask(room, coroutineContext)
            RoomConfig.RecordMode.STRAND -> StrandRecordTask(room, coroutineContext)
            else -> throw IllegalArgumentException("Unsupported recordMode for room ${room.roomConfig.roomId} with mode ${room.roomConfig.recordMode}")
        }
}