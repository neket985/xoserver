package ru.simpleteam.xoserver.entitie


data class XOSetRequest(
        val num: Int
): XORequest

class XORestartRequest: XORequest

interface XORequest

class XOCommand(val clazz: Class<out XORequest>){
    companion object{
        val SET = XOCommand(XOSetRequest::class.java)
        val RESTART = XOCommand(XORestartRequest::class.java)

        val values = mapOf(
                "set" to SET,
                "restart" to RESTART
        )
        fun valueOf(command: String) = values[command.toLowerCase()]
    }
}