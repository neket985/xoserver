package ru.simpleteam.xoserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class XoserverApplication

fun main(args: Array<String>) {
    runApplication<XoserverApplication>(*args)
}
