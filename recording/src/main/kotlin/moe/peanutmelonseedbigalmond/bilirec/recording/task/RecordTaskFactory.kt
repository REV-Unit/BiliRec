package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.DefaultRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.DiagnosisRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.StrandRecordTask
import kotlin.coroutines.CoroutineContext

object RecordTaskFactory {
    fun getRecordTask(room: Room, coroutineContext: CoroutineContext): BaseVideoRecordTask =
        when (room.roomConfig.recordMode) {
            RoomConfig.RecordMode.RAW -> DefaultRecordTask(room, coroutineContext)
            RoomConfig.RecordMode.STRAND -> StrandRecordTask(room, coroutineContext)
            RoomConfig.RecordMode.DIAGNOSIS -> DiagnosisRecordTask(room, coroutineContext)
            else -> throw IllegalArgumentException("Unsupported recordMode for room ${room.roomConfig.roomId} with mode ${room.roomConfig.recordMode}")
        }
}