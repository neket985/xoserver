package ru.simpleteam.xoserver.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import ru.simpleteam.xoserver.entitie.XOState
import java.util.concurrent.ConcurrentHashMap

@Service
class PingUsersService(
        @Qualifier("usersMap")
        private val usersMap: ConcurrentHashMap<String, XOState>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val pingedUsers = ConcurrentHashMap<WebSocketSession, XOState>()

    fun handlePong(sess: WebSocketSession) {
        pingedUsers.remove(sess)
    }

    @Scheduled(fixedDelay = 5_000)
    private fun ping() {
        pingedUsers.toList().toFlux().map { (sess, state) ->
            state.disconnect(sess)
        }.then(Mono.just(Unit).flatMap {
            pingedUsers.clear()
            usersMap.toList().toFlux().flatMapIterable { (key, state) ->
                if(state.players.isEmpty()){
                    usersMap.remove(key)
                }
                state.players.map { sess ->
                    pingedUsers.put(sess, state)
                    sess.send(
                            Flux.just(
                                    sess.pingMessage {
                                        it.allocateBuffer()
                                    }
                            )
                    ).onErrorResume {
                        logger.error("error while sending ws", it)
                        state.disconnect(sess)
                        Mono.create { it.success() }
                    }
                }
            }.flatMap { it }.then()
        }).block()
    }
}

fun main() {
    emptyList<Any>().toFlux().map {
        println("q")
    }.then(Mono.create<Void> {
        println("qq")
        it.success()
    }).block()
}