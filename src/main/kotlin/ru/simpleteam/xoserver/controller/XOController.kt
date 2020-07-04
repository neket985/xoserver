package ru.simpleteam.xoserver.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import ru.simpleteam.xoserver.config.WebSocketConfig
import ru.simpleteam.xoserver.controller.WsUtils.readXORequest
import ru.simpleteam.xoserver.controller.WsUtils.sendJson
import ru.simpleteam.xoserver.entitie.XOResponse
import ru.simpleteam.xoserver.entitie.XORestartRequest
import ru.simpleteam.xoserver.entitie.XOSetRequest
import ru.simpleteam.xoserver.entitie.XOState
import ru.simpleteam.xoserver.service.PingUsersService
import java.util.concurrent.ConcurrentHashMap

@Component
class XOController(
        private val pingService: PingUsersService,
        @Qualifier("usersMap")
        private val mainMap: ConcurrentHashMap<String, XOState>,
        private val mapper: ObjectMapper
) : WebSocketHandler {

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

        state.players.toFlux().flatMap {
            it.sendJson(state.toResponse())
        }.subscribe()

        val msgs = session.receive().map { msg ->
            when (msg.type) {
                WebSocketMessage.Type.PONG -> {
                    pingService.handlePong(session)
                    XOResponse.empty
                }
                WebSocketMessage.Type.BINARY, WebSocketMessage.Type.TEXT -> {
                    val request = try {
                        msg.readXORequest()
                    } catch (e: JsonProcessingException) {
                        logger.error("", e)
                        return@map XOResponse.jsonError(null)
                    } catch (e: IllegalArgumentException) {
                        logger.error("", e)
                        return@map XOResponse.jsonError(null)
                    }

                    when (request) {
                        is XOSetRequest -> synchronized(state) {
                            state.game(request.num, playerId)
                        }
                        is XORestartRequest -> synchronized(state) {
                            state.reset(playerId)
                        }
                        else -> XOResponse.clientError("Неизвестная команда", state.toData(), playerId)
                    }
                }
                else -> XOResponse.empty
            }
        }.filter { it != XOResponse.empty }.map {
            it!!.sessionId to mapper.writeValueAsString(it)
        } //todo попробовать без return чисто на sendJson

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
    private fun handleConnectPlayer(connId: String, session: WebSocketSession): XOState {
        val state = mainMap.getOrPut(connId) {
            XOState(XOState.initState(), mutableSetOf(session), session.id, null, mutableListOf(0, 0))
        }
        if (!state.players.contains(session)) {
            synchronized(state) {
                if (state.players.size >= 2) {
                    session.send(Flux.just(session.textMessage(mapper.writeValueAsString(XOResponse.lobbyFull())))).subscribe()
                    session.close(CloseStatus.BAD_DATA)
                }
                state.players.add(session)
            }
        }
        return state
    }
}