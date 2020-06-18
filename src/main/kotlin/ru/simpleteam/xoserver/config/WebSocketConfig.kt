package ru.simpleteam.xoserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerAdapter
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import ru.simpleteam.xoserver.controller.XOController


@Configuration
class WebSocketConfig {
    @Bean
    fun webSocketHandlerMapping(xoController: XOController): HandlerMapping? {
        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.order = 1
        handlerMapping.urlMap = mapOf("/xo" to xoController)
        return handlerMapping
    }

    @Bean
    fun wsHandlerAdapter(): HandlerAdapter? {
        return WebSocketHandlerAdapter()
    }
}