package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext

class StrandRecordTask(
     room: Room,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BaseRecordTask(room), CoroutineScope by CoroutineScope(coroutineContext) {
    private val startAndStopLock = ReentrantLock()

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean = mClosed

    @Volatile
    private var repairContext: LiveStreamRepairContext? = null

    override fun prepare() {
        // 准备时不需要做任何事
    }

    override fun startAsync(baseFileName: String) {
        startAndStopLock.lock()
        if (started) {
            startAndStopLock.unlock()
            return
        }
        runBlocking{ createLiveStreamRepairContextAsync() }
        repairContext = LiveStreamRepairContext(liveStream, room, baseFileName)
        repairContext!!.startAsync()
        started = true
        startAndStopLock.unlock()
        EventBus.getDefault().post(RecordFileOpenedEvent(this.room.roomConfig.roomId, baseFileName))
    }

    override fun stopRecording() {
        startAndStopLock.lock()
        if (!started) {
            startAndStopLock.unlock()
            return
        }
        started = false
        logger.info("停止接收直播流")
        repairContext?.close()
        repairContext = null
        startAndStopLock.unlock()
        EventBus.getDefault().post(RecordFileClosedEvent(this.room.roomConfig.roomId))
    }

    override fun close() {
        startAndStopLock.lock()
        if (closed) {
            startAndStopLock.unlock()
            return
        }
        mClosed = true
        stopRecording()
        cancel()
        startAndStopLock.unlock()
    }
}