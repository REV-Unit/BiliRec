package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import kotlin.coroutines.CoroutineContext

open class StrandRecordTask(
    room: Room,
    coroutineContext: CoroutineContext
) : DefaultRecordTask(room, coroutineContext) {
    override suspend fun setAndStartLiveStreamProcessContext(baseFileName: String) {
        liveStreamProcessContext =
            LiveStreamRepairContext(liveStream, room, baseFileName, this@StrandRecordTask.scope.coroutineContext)
        liveStreamProcessContext!!.start()
    }
}