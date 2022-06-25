package moe.peanutmelonseedbigalmond.bilirec.flv.exception

class FLVDataException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}