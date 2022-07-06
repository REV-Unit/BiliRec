package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.CoroutineContext

class StrandRecordTask(
    room: Room,
    coroutineContext: CoroutineContext
) : BaseRecordTask(room) {
    private val startAndStopLock = Mutex()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean = mClosed

    @Volatile
    private var repairContext: LiveStreamRepairContext? = null

    override suspend fun prepare() {
        // 准备时不需要做任何事
    }

    override suspend fun start(baseFileName: String) = withContext(scope.coroutineContext) {
        startAndStopLock.withLock {
            if (started) return@withLock
            createLiveStreamRepairContext()
            repairContext =
                LiveStreamRepairContext(liveStream, room, baseFileName, this@StrandRecordTask.scope.coroutineContext)
            repairContext!!.start()
            started = true
            EventBus.getDefault()
                .post(RecordFileOpenedEvent(this@StrandRecordTask.room.roomConfig.roomId, baseFileName))
        }
    }

    override suspend fun stopRecording() = withContext(scope.coroutineContext) {
        startAndStopLock.withLock {
            if (!started) return@withLock
            started = false
            logger.info("停止接收直播流")
            repairContext?.close()
            repairContext = null
            EventBus.getDefault().post(RecordFileClosedEvent(this@StrandRecordTask.room.roomConfig.roomId))
        }
    }

    override suspend fun close() {
        startAndStopLock.withLock {
            if (closed) return@withLock
            mClosed = true
            stopRecording()
            scope.cancel()
        }
    }
}