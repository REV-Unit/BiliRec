package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.CoroutineContext

class StrandRecordTask(
    room: Room,
) : BaseRecordTask(room) {
    private val startAndStopLock = Mutex()

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean = mClosed

    @Volatile
    private var repairContext: LiveStreamRepairContext? = null

    override suspend fun prepareAsync() {
        // 准备时不需要做任何事
    }

    override suspend fun startAsync(baseFileName: String) {
        startAndStopLock.withLock {
            if (started) return@withLock
            createLiveStreamRepairContextAsync()
            repairContext = LiveStreamRepairContext(liveStream, room, baseFileName)
            repairContext!!.startAsync()
            started = true
            EventBus.getDefault()
                .post(RecordFileOpenedEvent(this@StrandRecordTask.room.roomConfig.roomId, baseFileName))
        }
    }

    override suspend fun stopRecordingAsync() {
        startAndStopLock.withLock {
            if (!started) return@withLock
            started = false
            logger.info("停止接收直播流")
            repairContext?.closeAsync()
            repairContext = null
            EventBus.getDefault().post(RecordFileClosedEvent(this@StrandRecordTask.room.roomConfig.roomId))
        }
    }

    override suspend fun closeAsync() {
        startAndStopLock.withLock {
            if (closed) return@withLock
            mClosed = true
            stopRecordingAsync()
        }
    }
}