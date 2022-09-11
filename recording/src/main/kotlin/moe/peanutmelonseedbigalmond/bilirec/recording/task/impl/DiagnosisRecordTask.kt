package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.DiagnosisLiveStreamProcessContext
import kotlin.coroutines.CoroutineContext

class DiagnosisRecordTask(
    room: Room,
    coroutineContext: CoroutineContext
) : DefaultRecordTask(room, coroutineContext) {
    override suspend fun setAndStartLiveStreamProcessContext(baseFileName: String) {
        liveStreamProcessContext = DiagnosisLiveStreamProcessContext(
            liveStream,
            room,
            baseFileName,
            scope.coroutineContext
        )
        liveStreamProcessContext!!.start()
    }
}