package moe.peanutmelonseedbigalmond.bilirec.config

@kotlinx.serialization.Serializable
class ConfigRoot {
    var version: Int? = null
    var roomConfigs: List<RoomConfig>? = null

    companion object {
        private const val CURRENT_CONFIG_VERSION = 1

        val EMPTY_CONFIG = ConfigRoot().also {
            it.version = CURRENT_CONFIG_VERSION
            it.roomConfigs = ArrayList()
        }
    }
}