package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagwriter.DiagnosisFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class DiagnosisLiveStreamProcessContext(
    inputStream: InputStream,
    room: Room,
    outputFileNamePrefix: String,
    coroutineContext: CoroutineContext,
) : DefaultStreamProcessContext(inputStream, room, outputFileNamePrefix, coroutineContext) {
    override fun createFlvTagWriter(): BaseFlvTagWriter = DiagnosisFlvTagWriter("${outputFileNamePrefix}_data")
}