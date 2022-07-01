package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BaseRecordTask(room), CoroutineScope by CoroutineScope(coroutineContext) {
    private val startAndStopLock = Mutex()

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
        runBlocking(coroutineContext) {
            startAndStopLock.withLock {
                if (started) return@withLock
                createLiveStreamRepairContextAsync()
                repairContext = LiveStreamRepairContext(liveStream, room, baseFileName)
                repairContext!!.startAsync()
                started = true
                EventBus.getDefault().post(RecordFileOpenedEvent(this@StrandRecordTask.room.roomConfig.roomId, baseFileName))
            }
        }
    }

    override fun stopRecording() {
        runBlocking(coroutineContext) {
            startAndStopLock.withLock{
                if (!started) return@withLock
                started = false
                logger.info("停止接收直播流")
                repairContext?.close()
                repairContext = null
                EventBus.getDefault().post(RecordFileClosedEvent(this@StrandRecordTask.room.roomConfig.roomId))
            }
        }
    }

    override fun close() {
        runBlocking {
            startAndStopLock.withLock{
                if (closed)return@withLock
                mClosed = true
                stopRecording()
                this@StrandRecordTask.cancel()
            }
        }
    }
}