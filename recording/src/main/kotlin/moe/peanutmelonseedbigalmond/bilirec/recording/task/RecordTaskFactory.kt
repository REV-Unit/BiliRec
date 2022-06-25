package moe.peanutmelonseedbigalmond.bilirec.recording.task

import kotlinx.coroutines.Dispatchers
import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.RawRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.StrandRecordTask
import kotlin.coroutines.CoroutineContext

object RecordTaskFactory {
    fun getRecordTask(room: Room, coroutineScope: CoroutineContext = Dispatchers.IO): BaseRecordTask =
        when (room.roomConfig.recordMode) {
            RoomConfig.RecordMode.RAW -> RawRecordTask(room, coroutineScope)
            RoomConfig.RecordMode.STRAND -> StrandRecordTask(room, coroutineScope)
            else -> throw IllegalArgumentException("Unsupported recordMode for room ${room.roomConfig.roomId} with mode ${room.roomConfig.recordMode}")
        }
}