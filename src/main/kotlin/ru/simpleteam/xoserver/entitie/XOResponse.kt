package ru.simpleteam.xoserver.entitie

data class XOResponse(
        val code: Int,
        val msg: String?,
        val data: XOStateData?,
        val sessionId: String? = null
){
    companion object{
        val empty = XOResponse(0,null, null, null) //fixme

        fun ok(msg: String?, data: XOStateData?, sessionId: String? = null) =
                XOResponse(0, msg, data, sessionId)

        fun resetGame(data: XOStateData?, sessionId: String? = null) =
                XOResponse(300, "Игрок $sessionId перезапустил игру", data, null)

        fun info(msg: String?, data: XOStateData?, sessionId: String? = null) =
                XOResponse(201, msg, data, sessionId)

        fun jsonError(data: XOStateData?, sessionId: String? = null) =
                XOResponse(512, "Ошибка json-а", data, sessionId)

        fun lobbyFull(sessionId: String? = null) =
                XOResponse(410, "В текущем подключении уже присутствуют два игрока", null, sessionId)

        fun clientError(msg: String?, data: XOStateData?, sessionId: String? = null) =
                XOResponse(400, "Ошибка клиента: $msg", data, sessionId)

        fun serverError(msg: String?, data: XOStateData?, sessionId: String? = null) =
                XOResponse(500, "Ошибка сервера: $msg", data, sessionId)
    }
}
