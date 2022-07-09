package moe.peanutmelonseedbigalmond.bilirec.web.controller

import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import moe.peanutmelonseedbigalmond.bilirec.events.RoomAddEvent
import moe.peanutmelonseedbigalmond.bilirec.events.RoomDeleteEvent
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Recording
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.web.bean.request.AddRoomRequest
import moe.peanutmelonseedbigalmond.bilirec.web.bean.response.AddRoomResponse
import moe.peanutmelonseedbigalmond.bilirec.web.bean.response.DeleteRoomResponse
import moe.peanutmelonseedbigalmond.bilirec.web.bean.response.QueryRoomsResponse
import org.greenrobot.eventbus.EventBus
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class RoomsController {
    @GetMapping("/rooms")
    fun queryRooms(living: String?): QueryRoomsResponse {
        val roomData = when (living?.lowercase(Locale.getDefault())) {
            "true" -> getLivingRooms()
            "false" -> getNotLivingRooms()
            else -> getAllRooms()
        }
        return QueryRoomsResponse(roomData.map(QueryRoomsResponse.RoomResponse::fromRoom))
    }

    @PostMapping("/rooms")
    fun addRooms(resp: HttpServletResponse, @RequestBody addRoomRequest: AddRoomRequest): AddRoomResponse {
        return try {
            val roomConfig = addRoomRequest.toRoomConfig()
            Recording.INSTANCE.registerTaskAsync(Room(roomConfig, Dispatchers.IO))
            EventBus.getDefault().post(RoomAddEvent(addRoomRequest.toRoomConfig()))
            LoggingFactory.getLogger(addRoomRequest.roomId).info("已添加任务")
            AddRoomResponse("ok")
        } catch (e: IllegalArgumentException) {
            resp.status = HttpStatus.BAD_REQUEST.value()
            return AddRoomResponse(e.localizedMessage)
        }
    }

    @DeleteMapping("/rooms/{roomId}")
    fun deleteRoom(resp: HttpServletResponse, @PathVariable("roomId") roomId: Long): DeleteRoomResponse {
        return runBlocking {
            val roomDeleted = Recording.INSTANCE.unregisterTaskAsync(roomId)
            return@runBlocking if (roomDeleted != null) {
                EventBus.getDefault().post(RoomDeleteEvent(roomDeleted.roomConfig.roomId))
                LoggingFactory.getLogger(roomId).info("已移除任务")
                DeleteRoomResponse("ok")
            } else {
                resp.status = 404
                DeleteRoomResponse("找不到请求的直播间: $roomId")
            }
        }
    }


    private fun getAllRooms(): List<Room> {
        return Recording.INSTANCE.getAllTasks()
    }

    private fun getLivingRooms(): List<Room> {
        return getAllRooms().filter { it.living }
    }

    private fun getNotLivingRooms(): List<Room> {
        return getAllRooms().filter { !it.living }
    }
}