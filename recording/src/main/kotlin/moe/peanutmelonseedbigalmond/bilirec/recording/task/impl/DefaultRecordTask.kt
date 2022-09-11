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
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.DefaultStreamProcessContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseVideoRecordTask
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.CoroutineContext

/**
 *
 * 录制原始数据，不修复
 */
open class DefaultRecordTask(
    room: Room,
    coroutineContext: CoroutineContext
) : BaseVideoRecordTask(room) {
    private val startAndStopLock = Mutex()
    protected val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean
        get() = mClosed

    override suspend fun prepare() {
        // 准备时不需要做任何事
    }

    override suspend fun start(baseFileName: String) = startAndStopLock.withLock {
        withContext(scope.coroutineContext) {
            if (started) return@withContext
            createLiveStreamRepairContext()
            setAndStartLiveStreamProcessContext(baseFileName)
            started = true
            EventBus.getDefault()
                .post(RecordFileOpenedEvent(this@DefaultRecordTask.room.roomConfig.roomId, baseFileName))
        }
    }

    open suspend fun setAndStartLiveStreamProcessContext(baseFileName: String) {
        liveStreamProcessContext =
            DefaultStreamProcessContext(liveStream, room, baseFileName, this@DefaultRecordTask.scope.coroutineContext)
        liveStreamProcessContext!!.start()
    }

    override suspend fun stopRecording() {
        if (!started) return
        started = false
        logger.info("停止接收直播流")
        liveStreamProcessContext?.close()
        liveStreamProcessContext = null
        EventBus.getDefault().post(RecordFileClosedEvent(this@DefaultRecordTask.room.roomConfig.roomId))
    }

    override suspend fun close() {
        if (closed) return
        mClosed = true
        stopRecording()
        scope.cancel()
    }
}