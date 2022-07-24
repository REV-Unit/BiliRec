package moe.peanutmelonseedbigalmond.bilirec.recording.writer

import com.sun.xml.txw2.output.IndentingXMLStreamWriter
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.writeXmlElement
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.Duration
import java.time.OffsetDateTime
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class DanmakuWriter(
    private val room: Room,
    baseFileName: String
) : AutoCloseable {
    private val startTime: OffsetDateTime = OffsetDateTime.now()
    private var writer: XMLStreamWriter
    private var outputStream: OutputStream = FileOutputStream("$baseFileName.xml")

    @Volatile
    private var closed = false
    private var writeCount = 0

    init {
        writer = IndentingXMLStreamWriter(
            XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, Charsets.UTF_8.name())
        )
        writer.writeStartDocument()
        writer.writeProcessingInstruction("xml-stylesheet", """type="text/xsl" href="#s"""")
        /******<i>******/
        writer.writeComment(
            "本文件的弹幕信息兼容B站主站视频弹幕XML格式\n本XML自带样式可以在浏览器里打开（推荐使用Chrome）\n\nsc为SuperChat\ngift为礼物\nguard为上船\n\nattribute \"raw\" 为原始数据"
        )
        writer.writeStartElement("i")
        writer.writeXmlElement("chatserver") {
            text("chat.bilibili.com")
        }
        writer.writeXmlElement("chatid") {
            text("0")
        }
        writer.writeXmlElement("mission") {
            text("0")
        }
        writer.writeXmlElement("maxlimit") {
            text("1000")
        }
        writer.writeXmlElement("state") {
            text("0")
        }
        writer.writeXmlElement("real_name") {
            text("0")
        }
        writer.writeXmlElement("source") {
            text("0")
        }
        /******<BiliRec>******/
        writer.writeXmlElement("BiliRec") {
            attribute("version", "0.1")
        }
        /******</BiliRec>******/

        /******<BiliRecInfo>******/
        writer.writeXmlElement("BiliRecInfo") {
            attribute("roomid", room.roomConfig.roomId.toString())
            attribute("shortid", room.shortId.toString())
            attribute("name", removeInvalidXmlChars(room.userName))
            attribute("title", removeInvalidXmlChars(room.title))
            attribute("areanameparent", removeInvalidXmlChars(room.parentAreaName))
            attribute("areanamechild", removeInvalidXmlChars(room.childAreaName))
            attribute("start_time", startTime.toString())
        }
        /******</BiliRecInfo>******/

        /******<BiliRecXmlStyle>******/
        writeStyleSheet()
        /******</BiliRecXmlStyle>******/
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
        this.writer.writeXmlElement("d") {
            attribute(
                "p",
                "${String.format("%.3f", timestamp)}," +
                        "$danmakuType,$danmakuSize,$danmakuColor,$sendTime,0,${danmakuModel.userId},0"
            )
            attribute("user", removeInvalidXmlChars(danmakuModel.username))

            if (room.roomConfig.recordRawDanmakuData) {
                attribute("raw", danmakuModel.rawObject?.get("info")?.toString() ?: "")
            }

            text(removeInvalidXmlChars(danmakuModel.commentText ?: ""))
        }
        applyFlush()
    }

    @Synchronized
    fun writeSuperChatRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.SUPER_CHAT) return
        if (room.roomConfig.recordSuperChatData) {
            val timestamp =
                (Duration.between(startTime.toLocalDateTime(), OffsetDateTime.now().toLocalDateTime())
                    .toMillis() / 1000.0)
                    .coerceAtLeast(0.0)
            this.writer.writeXmlElement("sc") {
                attribute("ts", String.format("%.3f", timestamp))
                attribute("user", removeInvalidXmlChars(danmakuModel.username))
                attribute("price", danmakuModel.price.toString())
                attribute("time", danmakuModel.superChatKeepTime.toString())

                if (room.roomConfig.recordRawDanmakuData) {
                    attribute("raw", danmakuModel.rawObject?.get("data")?.toString() ?: "")
                }

                text(removeInvalidXmlChars(danmakuModel.commentText ?: ""))
            }
        }
        applyFlush()
    }

    @Synchronized
    fun writeGiftRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.SEND_GIFT) return
        if (room.roomConfig.recordSendGiftData) {
            val timestamp =
                (Duration.between(startTime.toLocalDateTime(), OffsetDateTime.now().toLocalDateTime())
                    .toMillis() / 1000.0)
                    .coerceAtLeast(0.0)
            this.writer.writeXmlElement("gift") {
                attribute("ts", String.format("%.3f", timestamp))
                attribute("user", removeInvalidXmlChars(danmakuModel.username))
                attribute("giftname", removeInvalidXmlChars(danmakuModel.giftName))
                attribute("giftcount", danmakuModel.giftCount.toString())

                if (room.roomConfig.recordRawDanmakuData) {
                    attribute("raw", danmakuModel.rawObject?.get("data")?.toString() ?: "")
                }
            }
        }
        applyFlush()
    }

    @Synchronized
    fun writeGuardBuyRecord(danmakuModel: DanmakuModel) {
        if (closed) return
        if (danmakuModel.command != DanmakuCommand.GUARD_BUY) return
        if (room.roomConfig.recordGuardByData) {
            val timestamp =
                (Duration.between(startTime.toLocalDateTime(), OffsetDateTime.now().toLocalDateTime())
                    .toMillis() / 1000.0)
                    .coerceAtLeast(0.0)
            this.writer.writeXmlElement("guard") {
                attribute("ts", String.format("%.3f", timestamp))
                attribute("user", removeInvalidXmlChars(danmakuModel.username))
                attribute("level", danmakuModel.userGuardLevel.toString())
                attribute("count", danmakuModel.giftCount.toString())
                if (room.roomConfig.recordRawDanmakuData) {
                    attribute("raw", danmakuModel.rawObject?.get("data")?.toString() ?: "")
                }
            }
        }
        applyFlush()
    }


    private fun applyFlush() {
        if (writeCount++ >= 10) {
            writer.flush()
            writeCount = 0
        }
    }

    private fun writeStyleSheet() {
        writer.writeXmlElement("BiliRecXmlStyle") {
            child("z:stylesheet") {

                attribute("version", "1.0")
                attribute("id", "s")
                attribute("xml:id", "s")
                attribute("xmlns:z", "http://www.w3.org/1999/XSL/Transform")

                child("z:output") {
                    attribute("method", "html")
                }
                child("z:template") {
                    attribute("match", "/")

                    child("html") {
                        child("meta") {
                            attribute("name", "viewport")
                            attribute("content", "width=device-width")
                        }
                        child("title") {
                            text("B站录播姬弹幕文件 -")
                            child("z:value-of") {
                                attribute("select", "/i/BiliRecInfo/@name")
                            }
                        }

                        child("style") {
                            text(
                                "body{margin:0}h1,h2,p,table{margin-left:5px}table{border-spacing:0}td,th{border:1px solid\n" +
                                        "grey;padding:1px}th{position:sticky;top:0;background:#4098de}tr:hover{background:#d9f4ff}div{overflow:auto;max-height:80vh;max-width:100vw;width:fit-content}"
                            )
                        }
                        child("h1") {
                            text("B站录播姬弹幕XML文件")
                        }
                        child("p") {
                            text("本文件的弹幕信息兼容B站主站视频弹幕XML格式，可以使用现有的转换工具把文件中的弹幕转为ass字幕文件")
                        }

                        child("table") {
                            child("tr") {
                                child("td") {
                                    text("录播姬版本")
                                }
                                child("td") {
                                    child("z:value-of") {
                                        attribute("select", "/i/BiliRec/@version")
                                    }
                                }
                            }

                            child("tr") {
                                child("td") {
                                    text("房间号")
                                }
                                child("td") {
                                    child("z:value-of") {
                                        attribute("select", "/i/BiliRecInfo/@roomid")
                                    }
                                }
                            }

                            child("tr") {
                                child("td") {
                                    text("主播名")
                                }
                                child("td") {
                                    child("z:value-of") {
                                        attribute("select", "/i/BiliRecInfo/@name")
                                    }
                                }
                            }

                            child("tr") {
                                child("td") {
                                    text("录制开始时间")
                                }
                                child("td") {
                                    child("z:value-of") {
                                        attribute("select", "/i/BiliRecInfo/@start_time")
                                    }
                                }
                            }

                            child("tr") {
                                child("td") {
                                    child("a") {
                                        attribute("href", "#d")
                                        text("弹幕")
                                    }
                                }
                                child("td") {
                                    text("共")
                                    child("z:value-of") {
                                        attribute("select", "count(/i/d)")
                                    }
                                    text(" 条记录")
                                }
                            }

                            child("tr") {
                                child("td") {
                                    child("a") {
                                        attribute("href", "#guard")
                                        text("上船")
                                    }
                                }
                                child("td") {
                                    text("共")
                                    child("z:value-of") {
                                        attribute("select", "count(/i/guard)")
                                    }
                                    text(" 条记录")
                                }
                            }

                            child("tr") {
                                child("td") {
                                    child("a") {
                                        attribute("href", "#sc")
                                        text("SC")
                                    }
                                }
                                child("td") {
                                    text("共")
                                    child("z:value-of") {
                                        attribute("select", "count(/i/sc)")
                                    }
                                    text(" 条记录")
                                }
                            }

                            child("tr") {
                                child("td") {
                                    child("a") {
                                        attribute("href", "#gift")
                                        text("礼物")
                                    }
                                }
                                child("td") {
                                    text("共")
                                    child("z:value-of") {
                                        attribute("select", "count(/i/gift)")
                                    }
                                    text(" 条记录")
                                }
                            }
                        }

                        child("h2") {
                            attribute("id", "d")
                            text("弹幕")
                        }
                        child("div") {
                            child("table") {
                                child("tr") {
                                    child("th") {
                                        text("用户名")
                                    }
                                    child("th") {
                                        text("弹幕")
                                    }
                                    child("th") {
                                        text("参数")
                                    }
                                }

                                child("z:for-each") {
                                    attribute("select", "/i/d")
                                    child("tr") {
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@user")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", ".")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@p")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        child("h2") {
                            attribute("id", "guard")
                            text("舰长购买")
                        }
                        child("div") {
                            child("table") {
                                child("tr") {
                                    child("th") {
                                        text("用户名")
                                    }
                                    child("th") {
                                        text("舰长等级")
                                    }
                                    child("th") {
                                        text("购买数量")
                                    }
                                    child("th") {
                                        text("出现时间")
                                    }
                                }

                                child("z:for-each") {
                                    attribute("select", "/i/guard")
                                    child("tr") {
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@user")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@level")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@count")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@ts")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        child("h2") {
                            attribute("id", "sc")
                            text("SuperChat 醒目留言")
                        }
                        child("div") {
                            child("table") {
                                child("tr") {
                                    child("th") {
                                        text("用户名")
                                    }
                                    child("th") {
                                        text("内容")
                                    }
                                    child("th") {
                                        text("显示时长")
                                    }
                                    child("th") {
                                        text("价格")
                                    }
                                    child("th") {
                                        text("出现时间")
                                    }
                                }

                                child("z:for-each") {
                                    attribute("select", "/i/sc")
                                    child("tr") {
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@user")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", ".")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@time")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@price")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@ts")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        child("h2") {
                            attribute("id", "gift")
                            text("礼物")
                        }
                        child("div") {
                            child("table") {
                                child("tr") {
                                    child("th") {
                                        text("用户名")
                                    }
                                    child("th") {
                                        text("礼物名")
                                    }
                                    child("th") {
                                        text("礼物数量")
                                    }
                                    child("th") {
                                        text("出现时间")
                                    }
                                }

                                child("z:for-each") {
                                    attribute("select", "/i/gift")
                                    child("tr") {
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@user")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@giftname")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@giftcount")
                                            }
                                        }
                                        child("td") {
                                            child("z:value-of") {
                                                attribute("select", "@ts")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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