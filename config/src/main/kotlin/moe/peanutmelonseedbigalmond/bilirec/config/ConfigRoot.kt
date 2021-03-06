package moe.peanutmelonseedbigalmond.bilirec.config

class ConfigRoot {
    var version: Int? = null
    var roomConfigs: List<RoomConfig>? = null

    companion object {
        val EMPTY_CONFIG = ConfigRoot().also {
            it.version = 1
            it.roomConfigs = ArrayList()
        }
    }
}