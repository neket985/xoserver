package ru.simpleteam.xoserver.entitie

import org.springframework.web.reactive.socket.WebSocketSession

data class XOState(
        val state: Array<Int>,
        val players: MutableSet<WebSocketSession>,
        var currentPlayer: String,
        var winner: String?
) {
    fun isReadyToStart() = players.size == 2
    fun isFinished() = winner != null

    fun toResponse() = XOResponse(
            null,
            toData()
    )
    fun toData() = XOStateData(
            state.toList(),
            players.map{it.id},
            currentPlayer,
            winner
    )
}

data class XOStateData(
        val state: List<Int>,
        val players: List<String>,
        val currentPlayer: String,
        val winner: String?
)
