package ru.simpleteam.xoserver.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import ru.simpleteam.xoserver.controller.WsUtils.readJsonTree
import ru.simpleteam.xoserver.entitie.XOCommand
import ru.simpleteam.xoserver.entitie.XORequest

object WsUtils {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun <T> WebSocketSession.sendJson(data: T) = this.sendJson(Flux.just(data))
    fun <T> WebSocketSession.sendJson(data: Flux<T>) = this.send(data.map {
        this.textMessage(mapper.writeValueAsString(it))
    })

    fun <T> WebSocketMessage.readJson(clazz: Class<T>) = mapper.readValue<T>(this.payload.asInputStream(), clazz)
    fun WebSocketMessage.readJsonTree() = mapper.readTree(this.payload.asInputStream())

    fun WebSocketMessage.readXORequest(): XORequest {
        val tree = readJsonTree()
        if (tree.hasNonNull("command") && tree.hasNonNull("data")) {
            val command = XOCommand.valueOf(tree["command"].asText())
                    ?: throw IllegalArgumentException("Command is invalid")
            return mapper.treeToValue(tree["data"], command.clazz)
        } else throw IllegalArgumentException("Command is missed")
    }

}