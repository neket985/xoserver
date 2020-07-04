package ru.simpleteam.xoserver.entitie

import org.springframework.web.reactive.socket.WebSocketSession
import ru.simpleteam.xoserver.controller.WsUtils.sendJson

data class XOState(
        var state: Array<Int>,
        val players: MutableSet<WebSocketSession>,
        var currentPlayer: String,
        var winner: String?,
        val wins: MutableList<Int>
) {
    fun disconnect(player: WebSocketSession) {
        player.close().block()
        if (players.contains(player)) {
            players.remove(player)
            if (players.isEmpty()) {
                currentPlayer = ""
                winner = null
                state = initState()
                wins[0] = 0
                wins[1] = 0
            } else if (players.size == 1) {
                val otherPlayer = players.first()
                currentPlayer = otherPlayer.id
                winner = null
                state = initState()
                wins[0] = 0
                wins[1] = 0

                otherPlayer.sendJson(this.toResponse("Пользователь ${player.id} отключен")).block()
            }
        }
    }

    fun reset(playerId: String) =
            handleReadyOnly(null) {
                state = initState()
                currentPlayer = players.elementAt(wins.sum() % 2).id
                winner = null
                XOResponse.resetGame(toData(), playerId)
            }

    fun game(num: Int, playerId: String): XOResponse =
            handleReadyOnly(playerId) {
                if (isFinished()) return@handleReadyOnly XOResponse.info("Игра окончена", toData(), playerId)
                if (currentPlayer != playerId) return@handleReadyOnly XOResponse.info("Ожидается ход другого игрока", toData(), playerId)
                if (state[num] != -1) return@handleReadyOnly XOResponse.info("Указанная клетка занята", toData(), playerId)

                val playerIndex = players.indexOfFirst { it.id == playerId }
                if (playerIndex == -1) return@handleReadyOnly XOResponse.info("\\(``)/ странная фигня, не знаю, как так вышло", toData(), playerId)
                state[num] = playerIndex
                currentPlayer = players.elementAt((playerIndex + 1) % 2).id
                val winIndex = state.checkWinner()
                winner = winIndex?.let { players.elementAt(it).id }
                if(winIndex!=null){
                    ++wins[winIndex]
                }

                toResponse()
            }

    private fun handleReadyOnly(playerId: String?, handle: () -> XOResponse) =
            if (isReadyToStart()) {
                handle()
            } else XOResponse.info("Ожидается подключение", toData(), playerId)

    private fun Array<Int>.checkWinner(): Int? =
            if (
                    this[0] != -1 && this[0] == this[1] && this[0] == this[2] ||
                    this[0] != -1 && this[0] == this[3] && this[0] == this[6] ||
                    this[0] != -1 && this[0] == this[4] && this[0] == this[8]
            ) this[0]
            else if (
                    this[2] != -1 && this[2] == this[5] && this[2] == this[8] ||
                    this[2] != -1 && this[2] == this[4] && this[2] == this[6]
            ) this[2]
            else if (
                    this[8] != -1 && this[8] == this[7] && this[8] == this[6]
            ) this[8]
            else if (
                    this[4] != -1 && this[4] == this[1] && this[4] == this[7] ||
                    this[4] != -1 && this[4] == this[3] && this[4] == this[5]
            ) this[4]
            else null


    fun isReadyToStart() = players.size == 2
    fun isFinished() = winner != null

    fun toResponse(msg: String? = null) = XOResponse.ok(
            msg,
            toData()
    )

    fun toData() = XOStateData(
            state.toList(),
            players.map { it.id },
            currentPlayer,
            winner,
            wins.toList()
    )

    companion object{
        fun initState() = arrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1)
    }
}

data class XOStateData(
        val state: List<Int>,
        val players: List<String>,
        val currentPlayer: String,
        val winner: String?,
        val wins: List<Int>
)
