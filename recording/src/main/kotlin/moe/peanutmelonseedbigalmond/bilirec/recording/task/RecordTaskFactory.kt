package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.RawRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.StrandRecordTask
import java.lang.IllegalArgumentException

object RecordTaskFactory {
    fun getRecordTask(room: Room): BaseRecordTask =
        when (room.roomConfig.recordMode) {
            RoomConfig.RecordMode.RAW -> RawRecordTask(room)
            RoomConfig.RecordMode.STRAND -> StrandRecordTask(room)
            else -> throw IllegalArgumentException("Unsupported recordMode for room ${room.roomConfig.roomId} with mode ${room.roomConfig.recordMode}")
        }
}