package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.FlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareBuilder
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareContext
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingRuleChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.EndTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.GOPGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.HeaderTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.ScriptTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.ScriptTagNormalizeGroupProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.TagTimestampOffsetGroupProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.UpdateTagTimestampGroupProcessNode
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

open class DefaultStreamProcessContext(
    inputStream: InputStream,
    room: Room,
    outputFileNamePrefix: String,
    coroutineCtx: CoroutineContext,
) : BaseLiveStreamProcessContext(inputStream, room, outputFileNamePrefix, coroutineCtx) {
    override val flvTagGroupingRuleBuilder: TagGroupingRuleChain.Builder
        get() = TagGroupingRuleChain.Builder()
            .addRule(EndTagGroupingRule())
            .addRule(GOPGroupingRule())
            .addRule(HeaderTagGroupingRule())
            .addRule(ScriptTagGroupingRule())
            .setLogger(logger)
            .onTagGroupEmitted(::onTagGroupReceived)

    override fun createFlvTagReader(): FlvTagReader = FlvTagReader(inputStream, logger)

    override fun createFlvTagWriter(): BaseFlvTagWriter = FlvTagWriter("${outputFileNamePrefix}_raw.flv")
}