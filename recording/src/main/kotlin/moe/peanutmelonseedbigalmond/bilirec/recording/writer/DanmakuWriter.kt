package moe.peanutmelonseedbigalmond.bilirec.recording.writer

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import java.io.*
import java.time.Duration
import java.time.OffsetDateTime
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class DanmakuWriter(
    private val room: Room,
    private val startTime: OffsetDateTime,
    outputFileName: String
) : AutoCloseable {
    private var writer: XMLStreamWriter
    private var outputStream: OutputStream = FileOutputStream(outputFileName)

    @Volatile
    private var closed = false
    private var writeCount = 0

    init {
        writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, Charsets.UTF_8.name())
        writer.writeStartDocument()
        writer.writeProcessingInstruction("xml-stylesheet", """type="text/xsl" href="#s"""")
        /******<i>******/
        writer.writeComment(
            "本文件的弹幕信息兼容B站主站视频弹幕XML格式\n本XML自带样式可以在浏览器里打开（推荐使用Chrome）\n\nsc为SuperChat\ngift为礼物\nguard为上船\n\nattribute \"raw\" 为原始数据"
        )
        writer.writeStartElement("i")
        writer.writeElementString("chatserver", "chat.bilibili.com")
        writer.writeElementString("chatid", "0")
        writer.writeElementString("mission", "0")
        writer.writeElementString("maxlimit", "1000")
        writer.writeElementString("state", "0")
        writer.writeElementString("real_name", "0")
        writer.writeElementString("source", "0")

        /******<BiliRec>******/
        writer.writeStartElement("BiliRec")
        writer.writeAttribute("version", "0.1")
        writer.writeEndElement()
        /******</BiliRec>******/

        /******<BiliRecInfo>******/
        writer.writeStartElement("BiliRecInfo")
        writer.writeAttribute("roomid", room.roomConfig.roomId.toString())
        writer.writeAttribute("shortid", room.shortId.toString())
        writer.writeAttribute("name", removeInvalidXmlChars(room.userName))
        writer.writeAttribute("title", removeInvalidXmlChars(room.title))
        writer.writeAttribute("areanameparent", removeInvalidXmlChars(room.parentAreaName))
        writer.writeAttribute("areanamechild", removeInvalidXmlChars(room.childAreaName))
        writer.writeAttribute("start_time", startTime.toString())
        writer.writeEndElement()
        /******</BiliRecInfo>******/

        /******<BiliRecXmlStyle>******/
        writer.writeStartElement("BiliRecXmlStyle")
        val style =
            """<z:stylesheet version="1.0" id="s" xml:id="s" xmlns:z="http://www.w3.org/1999/XSL/Transform"><z:output method="html"/><z:template match="/"><html><meta name="viewport" content="width=device-width"/><title>B站录播姬弹幕文件 - <z:value-of select="/i/BiliRecInfo/@name"/></title><style>body{margin:0}h1,h2,p,table{margin-left:5px}table{border-spacing:0}td,th{border:1px solid grey;padding:1px}th{position:sticky;top:0;background:#4098de}tr:hover{background:#d9f4ff}div{overflow:auto;max-height:80vh;max-width:100vw;width:fit-content}</style><h1>B站录播姬弹幕XML文件</h1><p>本文件的弹幕信息兼容B站主站视频弹幕XML格式，可以使用现有的转换工具把文件中的弹幕转为ass字幕文件</p><table><tr><td>录播姬版本</td><td><z:value-of select="/i/BiliRec/@version"/></td></tr><tr><td>房间号</td><td><z:value-of select="/i/BiliRecInfo/@roomid"/></td></tr><tr><td>主播名</td><td><z:value-of select="/i/BiliRecInfo/@name"/></td></tr><tr><td>录制开始时间</td><td><z:value-of select="/i/BiliRecInfo/@start_time"/></td></tr><tr><td><a href="#d">弹幕</a></td><td>共 <z:value-of select="count(/i/d)"/> 条记录</td></tr><tr><td><a href="#guard">上船</a></td><td>共 <z:value-of select="count(/i/guard)"/> 条记录</td></tr><tr><td><a href="#sc">SC</a></td><td>共 <z:value-of select="count(/i/sc)"/> 条记录</td></tr><tr><td><a href="#gift">礼物</a></td><td>共 <z:value-of select="count(/i/gift)"/> 条记录</td></tr></table><h2 id="d">弹幕</h2><div><table><tr><th>用户名</th><th>弹幕</th><th>参数</th></tr><z:for-each select="/i/d"><tr><td><z:value-of select="@user"/></td><td><z:value-of select="."/></td><td><z:value-of select="@p"/></td></tr></z:for-each></table></div><h2 id="guard">舰长购买</h2><div><table><tr><th>用户名</th><th>舰长等级</th><th>购买数量</th><th>出现时间</th></tr><z:for-each select="/i/guard"><tr><td><z:value-of select="@user"/></td><td><z:value-of select="@level"/></td><td><z:value-of select="@count"/></td><td><z:value-of select="@ts"/></td></tr></z:for-each></table></div><h2 id="sc">SuperChat 醒目留言</h2><div><table><tr><th>用户名</th><th>内容</th><th>显示时长</th><th>价格</th><th>出现时间</th></tr><z:for-each select="/i/sc"><tr><td><z:value-of select="@user"/></td><td><z:value-of select="."/></td><td><z:value-of select="@time"/></td><td><z:value-of select="@price"/></td><td><z:value-of select="@ts"/></td></tr></z:for-each></table></div><h2 id="gift">礼物</h2><div><table><tr><th>用户名</th><th>礼物名</th><th>礼物数量</th><th>出现时间</th></tr><z:for-each select="/i/gift"><tr><td><z:value-of select="@user"/></td><td><z:value-of select="@giftname"/></td><td><z:value-of select="@giftcount"/></td><td><z:value-of select="@ts"/></td></tr></z:for-each></table></div></html></z:template></z:stylesheet>"""
        writer.writeRaw(style)
        writer.writeEndElement()
        writer.flush()
        /******</BiliRecXmlStyle>******/
    }

    private fun XMLStreamWriter.writeElementString(tagName: String, text: String = "") {
        this.writeStartElement(tagName)
        this.writeCharacters(text)
        this.writeEndElement()
    }

    @Synchronized
    fun writeDanmakuRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.DANMAKU) return
        val danmakuType = danmakuModel.rawObject?.getJSONArray("info")?.getJSONArray(0)?.getInt(1) ?: 1
        val danmakuSize = danmakuModel.rawObject?.getJSONArray("info")?.getJSONArray(0)?.getInt(2) ?: 25
        val danmakuColor =
            danmakuModel.rawObject?.getJSONArray("info")?.getJSONArray(0)?.getInt(3) ?: 0xff_ff_ff
        val sendTime = danmakuModel.rawObject?.getJSONArray("info")?.getJSONArray(0)?.getLong(4) ?: 0L
        val timestamp =
            (Duration.between(startTime.toLocalDateTime(), OffsetDateTime.now().toLocalDateTime())
                .toMillis() / 1000.0).coerceAtLeast(0.0)
        this.writer.writeStartElement("d")
        this.writer.writeAttribute(
            "p",
            "${
                String.format(
                    "%.3f",
                    timestamp
                )
            },$danmakuType,$danmakuSize,$danmakuColor,$sendTime,0,${danmakuModel.userId},0"
        )
        this.writer.writeAttribute("user", removeInvalidXmlChars(danmakuModel.username))

        if (room.roomConfig.recordRawDanmakuData) {
            this.writer.writeAttribute(
                "raw",
                danmakuModel.rawObject?.get("info")?.toString() ?: ""
            )
        }
        this.writer.writeCharacters(removeInvalidXmlChars(danmakuModel.commentText ?: ""))
        this.writer.writeEndElement()
        applyFlush()
    }

    @Synchronized
    fun writeSuperChatRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.SUPER_CHAT) return
        if (room.roomConfig.recordSuperChatData) {
            val timestamp =
                (Duration.between(
                    startTime.toLocalDateTime(),
                    OffsetDateTime.now().toLocalDateTime()
                )
                    .toMillis() / 1000.0).coerceAtLeast(0.0)
            this.writer.writeStartElement("sc")
            this.writer.writeAttribute("ts", String.format("%.3f", timestamp))
            this.writer.writeAttribute("user", removeInvalidXmlChars(danmakuModel.username))
            this.writer.writeAttribute("price", danmakuModel.price.toString())
            this.writer.writeAttribute("time", danmakuModel.superChatKeepTime.toString())

            if (room.roomConfig.recordRawDanmakuData) {
                this.writer.writeAttribute(
                    "raw",
                    danmakuModel.rawObject?.get("data")?.toString() ?: ""
                )
            }
            this.writer.writeEndElement()
        }
        applyFlush()
    }

    @Synchronized
    fun writeGiftRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.SEND_GIFT) return
        if (room.roomConfig.recordSendGiftData) {
            this.writer.writeStartElement("gift")
            val timestamp =
                (Duration.between(
                    startTime.toLocalDateTime(),
                    OffsetDateTime.now().toLocalDateTime()
                )
                    .toMillis() / 1000.0).coerceAtLeast(0.0)
            this.writer.writeAttribute("ts", String.format("%.3f", timestamp))
            this.writer.writeAttribute("user", removeInvalidXmlChars(danmakuModel.username))
            this.writer.writeAttribute("giftname", removeInvalidXmlChars(danmakuModel.giftName))
            this.writer.writeAttribute("giftcount", danmakuModel.giftCount.toString())
            if (room.roomConfig.recordRawDanmakuData) {
                this.writer.writeAttribute(
                    "raw",
                    danmakuModel.rawObject?.get("data")?.toString() ?: ""
                )
            }
            this.writer.writeEndElement()
        }
        applyFlush()
    }

    @Synchronized
    fun writeGuardBuyRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.GUARD_BUY) return
        if (room.roomConfig.recordGuardByData) {
            this.writer.writeStartElement("guard")
            val timestamp =
                (Duration.between(
                    startTime.toLocalDateTime(),
                    OffsetDateTime.now().toLocalDateTime()
                )
                    .toMillis() / 1000.0).coerceAtLeast(0.0)
            this.writer.writeAttribute("ts", String.format("%.3f", timestamp))
            this.writer.writeAttribute("user", removeInvalidXmlChars(danmakuModel.username))
            this.writer.writeAttribute("level", danmakuModel.userGuardLevel.toString())
            this.writer.writeAttribute("count", danmakuModel.giftCount.toString())
            if (room.roomConfig.recordRawDanmakuData) {
                this.writer.writeAttribute("raw", danmakuModel.rawObject?.get("data")?.toString());
            }
            this.writer.writeEndElement()
        }
        applyFlush()
    }


    private fun applyFlush() {
        if (writeCount++ >= 10) {
            writer.flush()
            writeCount = 0
        }
    }

    // 需要添加JVM参数 --add-opens java.xml/com.sun.xml.internal.stream.writers=ALL-UNNAMED
    private fun XMLStreamWriter.writeRaw(text: String) {
        if (this.javaClass.name == "com.sun.xml.internal.stream.writers.XMLStreamWriterImpl") {
            val clazz = this.javaClass
            val b1 = clazz.getDeclaredMethod("getEscapeCharacters").invoke(this) as Boolean
            clazz.getDeclaredMethod("setEscapeCharacters", Boolean::class.java).invoke(this, false)
            this.writeCharacters(text)
            clazz.getDeclaredMethod("setEscapeCharacters", Boolean::class.java).invoke(this, b1)
        } else {
            this.writeCharacters(text)
        }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        writer.writeEndElement()
        writer.flush()
        writer.close()
        outputStream.close()
        this.closed = true
    }

    companion object {
        private val invalidXmlChars =
            Regex("(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]|[\uD800-\uDBFF](?![\uDC00-\uDFFF])|[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x9F\uFEFF\uFFFE\uFFFF]")

        private fun removeInvalidXmlChars(text: String?): String =
            if (text.isNullOrBlank()) {
                ""
            } else {
                text.replace(invalidXmlChars, "")
            }
    }
}