package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class Recording private constructor(private val innerMap: ConcurrentHashMap<Long, Room>) :
    Map<Long, Room> by innerMap {

    suspend fun registerTaskAsync(room: Room, forceUpdate: Boolean = false):Unit = coroutineScope {
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
        coroutineScope {
            launch {
                innerMap.remove(roomId)?.close()
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
