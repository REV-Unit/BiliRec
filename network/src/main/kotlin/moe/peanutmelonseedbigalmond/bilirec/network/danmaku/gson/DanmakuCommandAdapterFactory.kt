package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.gson

import com.google.gson.*
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand
import java.lang.reflect.Type

object DanmakuCommandAdapterFactory : JsonSerializer<DanmakuCommand>, JsonDeserializer<DanmakuCommand> {
    override fun serialize(src: DanmakuCommand?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement? {
        if (src == null) return null
        return JsonPrimitive(src.command)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): DanmakuCommand? {
        if (json == null) return null
        return DanmakuCommand.parse(json.asString)
    }
}