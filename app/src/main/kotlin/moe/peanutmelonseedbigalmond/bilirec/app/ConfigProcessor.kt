package moe.peanutmelonseedbigalmond.bilirec.app

import moe.peanutmelonseedbigalmond.bilirec.RoomInfoRefreshEvent
import moe.peanutmelonseedbigalmond.bilirec.config.ConfigReader
import moe.peanutmelonseedbigalmond.bilirec.config.ConfigRoot
import moe.peanutmelonseedbigalmond.bilirec.config.ConfigWriter
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

object ConfigProcessor {
    const val DEFAULT_CONFIG_FILE_NAME = "config.yaml"
    const val CURRENT_CONFIG_VERSION = 1

    private lateinit var curConfig: ConfigRoot
    private lateinit var curConfigFile: File

    init {
        EventBus.getDefault().register(this)
    }

    fun loadConfig(configFile: File): ConfigRoot {
        curConfig = if (!configFile.exists()) {
            ConfigWriter.write(ConfigRoot.EMPTY_CONFIG, configFile)
            LoggingFactory.getLogger().warn("${configFile.canonicalPath} not found, created a new one")
            return loadConfig(configFile)
        } else {
            curConfigFile = configFile
            ConfigReader.readConfig(configFile)
        }
        return curConfig
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRoomInfoRefresh(event: RoomInfoRefreshEvent) {
        curConfig.roomConfigs?.forEach {
            if (it.roomId == event.shortId) {
                it.roomId = event.roomId
                it.title = event.title
            } else if (it.roomId == event.roomId) {
                it.title = event.title
            }
        }
        ConfigWriter.write(curConfig, curConfigFile)
    }
}