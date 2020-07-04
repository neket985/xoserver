package ru.simpleteam.xoserver.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux

open class CustomWSHandler(protected val mapper: ObjectMapper) {
    protected fun <T> WebSocketSession.sendJson(data: T) = this.sendJson(Flux.just(data))
    protected fun <T> WebSocketSession.sendJson(data: Flux<T>) = this.send(data.map { this.textMessage(mapper.writeValueAsString(data)) })

    protected fun <T> WebSocketMessage.readJson(clazz: Class<T>) = mapper.readValue<T>(this.payload.asInputStream(), clazz)
    protected fun WebSocketMessage.readJsonTree() = mapper.readTree(this.payload.asInputStream())

}