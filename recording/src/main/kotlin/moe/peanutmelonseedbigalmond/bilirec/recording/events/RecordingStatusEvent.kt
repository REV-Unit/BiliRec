package moe.peanutmelonseedbigalmond.bilirec.recording.events

class RecordingStatusEvent {
    var inputVideoByteCount: Long = 0
    var inputAudioByteCount: Long = 0

    var outputVideoFrameCount: Int = 0
    var outputAudioFrameCount: Int = 0
    var outputVideoByteCount: Long = 0
    var outputAudioByteCount: Long = 0

    var totalInputVideoByteCount: Long = 0
    var totalInputAudioByteCount: Long = 0

    var totalOutputVideoFrameCount: Int = 0
    var totalOutputAudioFrameCount: Int = 0
    var totalOutputVideoByteCount: Long = 0
    var totalOutputAudioByteCount: Long = 0

    var currentFileSize: Long = 0

    var addedDuration: Double = 0.0
    var passedTime: Double = 0.0
    var durationRatio: Double = 0.0
    var sessionMaxTimestamp: Int = 0
    var fileMaxTimestamp: Int = 0
}