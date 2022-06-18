package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain

abstract class BaseFlvTagProcessNode<R> {
    abstract fun proceed(chain: FlvTagProcessChain<R>, tag: R)
}