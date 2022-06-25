package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data

import struct.StructClass
import struct.StructField

@StructClass
class DanmakuMessageData {
    @StructField(order = 0)
    var packetLength: Int = 0

    @StructField(order = 1)
    var headerLength: Short = 16

    @StructField(order = 2)
    var version: Short = 0

    @StructField(order = 3)
    var operationCode: Int = 0

    @StructField(order = 4)
    var magic: Int = 1

    @StructField(order = 5)
    var body: ByteArray = byteArrayOf()
}