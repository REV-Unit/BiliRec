package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.DiagnosisFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingProcessChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.EndTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.GOPGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.HeaderTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.ScriptTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class DiagnosisLiveStreamProcessContext(
    inputStream: InputStream,
    room: Room,
    outputFileNamePrefix: String,
    coroutineContext: CoroutineContext
) : BaseLiveStreamProcessContext(inputStream, room, outputFileNamePrefix, coroutineContext) {
    override val logger: BaseLogging
        get() = LoggingFactory.getLogger(room.roomId, this)

    override fun createFlvTagWriter(): BaseFlvTagWriter {
        return DiagnosisFlvTagWriter(outputFileNamePrefix)
    }

    override fun createFlvTagReader(): FlvTagReader {
        return FlvTagReader(inputStream, logger)
    }

    override fun createTagProcessChainWithoutAction(): FlvTagProcessChain<List<Tag>> {
        return FlvTagProcessChain(logger)
    }

    override fun createTagGroupingProcessChainWithoutAction(): TagGroupingProcessChain {
        return TagGroupingProcessChain(this.logger)
            .addRule(ScriptTagGroupingRule())
            .addRule(EndTagGroupingRule())
            .addRule(HeaderTagGroupingRule())
            .addRule(GOPGroupingRule())
    }

    override fun onTagGroupRead(tagGroup: List<Tag>) {
        for (tag in tagGroup) {
            when (tag.getTagType()) {
                TagType.SCRIPT -> flvTagWriter!!.writeFlvScriptTag(tag)
                TagType.VIDEO, TagType.AUDIO -> flvTagWriter!!.writeFlvData(tag)
                else -> {}
            }
        }
    }
}