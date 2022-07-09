package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Recording private constructor(private val set: HashSet<Room>) : MutableSet<Room> by set {

    fun registerTaskAsync(room: Room) {
        set.add(room)
        room.prepare()
    }

    suspend fun unregisterTaskAsync(roomId: Long): Room? {
        return withContext(Dispatchers.IO) {
            val roomToRemove = set.firstOrNull { it.roomConfig.roomId == roomId || it.shortId == roomId }
            roomToRemove?.close()
            this@Recording.remove(roomToRemove)
            return@withContext roomToRemove
        }
    }

    suspend fun unregisterAllTasksAsync() {
        for (room in this) {
            unregisterTaskAsync(room.roomConfig.roomId)
        }
    }

    fun getAllTasks() = this.toList()

    companion object {
        val INSTANCE = Recording(HashSet())
    }
}
