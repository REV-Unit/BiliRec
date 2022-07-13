package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

object Recording {
    private val rooms: MutableList<Room> = mutableListOf()
    fun registerTaskAsync(room: Room) {
        if (hasRoom(room.roomId)) {
            return
        } else {
            rooms.add(room)
        }
    }

    fun hasRoom(id: Long): Boolean {
        return findRoom(id) != null
    }

    fun findRoom(id: Long): Room? {
        return rooms.find { it.roomId == id || it.shortId == id }
    }

    suspend fun unregisterTask(roomId: Long) {
        withContext(Dispatchers.IO) {
            val room = findRoom(roomId)
            room?.let {
                room.close()
                rooms.remove(room)
            }
        }
    }

    suspend fun unregisterAllTasksAsync() = supervisorScope {
        for (room in rooms) {
            launch { unregisterTask(room.roomId) }
        }
    }

    fun getAllTasks() = rooms
}
