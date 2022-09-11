package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.FlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareBuilder
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareContext
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingRuleChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.ScriptTagNormalizeGroupProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.UpdateTagTimestampGroupProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagwriter.RepairFlvTagWriter
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class LiveStreamRepairContext(
    inputStream: InputStream,
    room: Room,
    outputFileNamePrefix: String,
    coroutineContext: CoroutineContext
) : DefaultStreamProcessContext(inputStream, room, outputFileNamePrefix, coroutineContext) {
    private val flvTagGroupProcessChain = MiddlewareBuilder<TagGroup>()
        .use(ScriptTagNormalizeGroupProcessNode())
//        .use(TagTimestampOffsetGroupProcessNode())
        .use(UpdateTagTimestampGroupProcessNode())
        .build()
    override val flvTagGroupingRuleBuilder: TagGroupingRuleChain.Builder
        get() = super.flvTagGroupingRuleBuilder
            .onTagGroupEmitted(this::onTagGroupReceived)

    private val flvTagGroupProcessChainExtraData = mutableMapOf<String, Any>()

    override fun createFlvTagWriter(): BaseFlvTagWriter =
        RepairFlvTagWriter(room.roomId, FlvTagWriter("${outputFileNamePrefix}.flv"))

    override fun onTagGroupReceived(tagGroup: TagGroup) {
        flvTagGroupProcessChain(MiddlewareContext(tagGroup, flvTagGroupProcessChainExtraData))
        super.onTagGroupReceived(tagGroup)
    }
}
