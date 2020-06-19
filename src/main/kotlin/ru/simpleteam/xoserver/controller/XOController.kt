package ru.simpleteam.xoserver.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import ru.simpleteam.xoserver.config.WebSocketConfig
import ru.simpleteam.xoserver.entitie.XORequest
import ru.simpleteam.xoserver.entitie.XOResponse
import ru.simpleteam.xoserver.entitie.XOState
import java.util.concurrent.ConcurrentHashMap


@Component
class XOController : WebSocketHandler {
    private val mapper = jacksonObjectMapper()
    private val mainMap = ConcurrentHashMap<String, XOState>() //todo delete

    private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        println("connected")
        val params = session.handshakeInfo.uri.query.split("&").mapNotNull {
            val spl = it.split("=", limit = 2)
            if (spl.size != 2) null
            else spl[0] to spl[1]
        }.toMap()
        val connId = params.getOrElse("id") {
            throw IllegalArgumentException("Параметр id обязателен")
        }.toString()
        val playerId = session.id
        val state = handleConnectPlayer(connId, session)

        session.send(Flux.just(session.textMessage(mapper.writeValueAsString(state.toResponse())))).subscribe()

        val msgs = session.receive().map { msg ->
            val request = try {
                mapper.readValue(msg.payload.asInputStream(), XORequest::class.java)
            } catch (e: JsonProcessingException) {
                return@map XOResponse(e.message, null)
            }
            synchronized(state) {
                if (state.isReadyToStart()) {
                    if (state.isFinished()) return@map XOResponse("Игра окончена", state.toData(), playerId)
                    if (state.currentPlayer != playerId) return@map XOResponse("Ожидается ход другого игрока", state.toData(), playerId)
                    if (state.state[request.num] != -1) return@map XOResponse("Указанная клетка занята", state.toData(), playerId)

                    val playerIndex = state.players.indexOfFirst { it.id == playerId }
                    if (playerIndex == -1) return@map XOResponse("\\(``)/ странная фигня, не знаю, как так вышло", state.toData(), playerId)
                    state.state[request.num] = playerIndex
                    state.currentPlayer = state.players.elementAt((playerIndex + 1) % 2).id
                    state.winner = state.state.checkWinner()?.let { state.players.elementAt(it).id }

                    state.toResponse()
                } else return@map XOResponse("Ожидается подключение", state.toData(), playerId)
            }
        }.map {
            it.sessionId to mapper.writeValueAsString(it)
        }

        return msgs.flatMap { (key, fl) ->
            val msg = Flux.just(fl)
            if (key != null) {
                val sess = state.players.firstOrNull { it.id == key }
                sess?.send(msg.map { sess.textMessage(it) })
            } else {
                state.players.toFlux().flatMap { sess ->
                    sess.send(msg.map { sess.textMessage(it) })
                }.then()
            }
        }.then()
                .onErrorMap {
                    logger.error("err", it)
                    it
                }
    }

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

    private fun handleConnectPlayer(connId: String, session: WebSocketSession): XOState {
        val state = mainMap.getOrPut(connId) {
            XOState(arrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1), mutableSetOf(session), session.id, null)
        }
        if (!state.players.contains(session)) {
            synchronized(state) {
                if (state.players.size >= 2) {
                    session.send(Flux.just(session.textMessage(mapper.writeValueAsString(XOResponse("В текущем подключении уже присутствуют два игрока", null))))).subscribe()
                    session.close(CloseStatus.BAD_DATA)
                }
                state.players.add(session)
            }
        }
        return state
    }
}