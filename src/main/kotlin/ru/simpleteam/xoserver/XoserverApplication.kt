package ru.simpleteam.xoserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class XoserverApplication

fun main(args: Array<String>) {
    runApplication<XoserverApplication>(*args)
}
