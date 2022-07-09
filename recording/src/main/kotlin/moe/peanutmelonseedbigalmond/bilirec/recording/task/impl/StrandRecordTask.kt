package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseVideoRecordTask
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.CoroutineContext

class StrandRecordTask(
    room: Room,
    coroutineContext: CoroutineContext
) : BaseVideoRecordTask(room) {
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

    override suspend fun start(baseFileName: String) = startAndStopLock.withLock {
        withContext(scope.coroutineContext) {
            if (started) return@withContext
            createLiveStreamRepairContext()
            repairContext =
                LiveStreamRepairContext(liveStream, room, baseFileName, this@StrandRecordTask.scope.coroutineContext)
            repairContext!!.start()
            started = true
            EventBus.getDefault()
                .post(RecordFileOpenedEvent(this@StrandRecordTask.room.roomConfig.roomId, baseFileName))
        }
    }

    override suspend fun stopRecording() = startAndStopLock.withReentrantLock {
        withContext(scope.coroutineContext) {
            if (!started) return@withContext
            started = false
            logger.info("停止接收直播流")
            repairContext?.close()
            repairContext = null
            EventBus.getDefault().post(RecordFileClosedEvent(this@StrandRecordTask.room.roomConfig.roomId))
        }
    }

    override suspend fun close() {
        startAndStopLock.withReentrantLock {
            if (closed) return@withReentrantLock
            mClosed = true
            stopRecording()
            scope.cancel()
        }
    }
}