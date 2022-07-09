package moe.peanutmelonseedbigalmond.bilirec.recording.task

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.extension.getCodecItemInStreamUrlAsync
import java.io.InputStream
import java.time.Duration
import kotlin.coroutines.coroutineContext

abstract class BaseRecordTask(protected val room: Room) : SuspendableCloseable {
    abstract val closed: Boolean
    protected open val logger = LoggingFactory.getLogger(room.roomConfig.roomId, this)
    abstract suspend fun prepare()
    abstract suspend fun start(baseFileName: String)

    // 结束录制，但是不结束任务
    abstract suspend fun stopRecording()
}