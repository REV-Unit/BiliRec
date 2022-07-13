package moe.peanutmelonseedbigalmond.bilirec.recording

import java.time.OffsetDateTime

fun formatFileName(roomId: Long, date: String, time: String, ns: String, title: String) =
    "录制-$roomId-$date-$time-$ns-$title"

fun generateFileName(room: Room, offsetDateTime: OffsetDateTime = OffsetDateTime.now()): String {
    val time = offsetDateTime.toLocalDateTime()
    val date =
        time.year.toString() + String.format("%02d", time.month.value) + String.format("%02d", time.dayOfMonth)
    val t =
        String.format("%02d", time.hour) + String.format("%02d", time.minute) + String.format(
            "%02d",
            time.second
        )
    return formatFileName(
        room.roomId,
        date,
        t,
        offsetDateTime.nano.toString(),
        room.title
    )
}