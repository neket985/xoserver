package ru.simpleteam.xoserver.entitie

data class XOResponse(
        val error: String?,
        val data: XOStateData?,
        val sessionId: String? = null
)
