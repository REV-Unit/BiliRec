package moe.peanutmelonseedbigalmond.bilirec.recording

import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class Recording private constructor(private val innerMap: ConcurrentHashMap<Long, Room>) :
    Map<Long, Room> by innerMap {

    fun registerTask(room: Room, forceUpdate: Boolean = false) {
        if (forceUpdate) {
            if (innerMap.contains(room.roomConfig.roomId)) {
                unregisterTask(room.roomConfig.roomId)
                registerTask(room, false)
            }
        } else {
            if (!innerMap.contains(room.roomConfig.roomId)) {
                innerMap[room.roomConfig.roomId] = room
            }
        }
    }

    fun unregisterTask(roomId: Long) {
        thread {
            innerMap.remove(roomId)?.close()
        }
    }

    fun unregisterAllTasks() {
        for (key in getAllTasks()) {
            unregisterTask(key)
        }
    }

    fun getAllTasks() = innerMap.keys().toList()

    companion object {
        val INSTANCE = Recording(ConcurrentHashMap())
    }
}
