package moe.peanutmelonseedbigalmond.bilirec.recording.task

import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.StrandRecordTask

object RecordTaskFactory {
    fun getRecordTask(room: Room):BaseRecordTask=StrandRecordTask(room)
}