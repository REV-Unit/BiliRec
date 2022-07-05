package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import moe.peanutmelonseedbigalmond.bilirec.closeQuietlyAsync
import java.util.concurrent.ConcurrentHashMap

class Recording private constructor(private val innerMap: ConcurrentHashMap<Long, Room>) :
    Map<Long, Room> by innerMap {

    suspend fun registerTaskAsync(room: Room, forceUpdate: Boolean = false) {
        if (forceUpdate) {
            if (innerMap.contains(room.roomConfig.roomId)) {
                unregisterTaskAsync(room.roomConfig.roomId)
                registerTaskAsync(room, false)
            }
        } else {
            if (!innerMap.contains(room.roomConfig.roomId)) {
                innerMap[room.roomConfig.roomId] = room
                room.prepareAsync()
            }
        }
    }

    suspend fun unregisterTaskAsync(roomId: Long) {
        withContext(Dispatchers.IO) {
            innerMap.remove(roomId)?.let {
                it.closeAsync()
                it.cancel()
            }
        }
    }

    suspend fun unregisterAllTasksAsync() {
        for (key in getAllTasks()) {
            unregisterTaskAsync(key)
        }
    }

    fun getAllTasks() = innerMap.keys().toList()

    companion object {
        val INSTANCE = Recording(ConcurrentHashMap())
    }
}
