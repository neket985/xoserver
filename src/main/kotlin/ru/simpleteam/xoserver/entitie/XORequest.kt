package ru.simpleteam.xoserver.entitie


data class XOSetRequest(
        override val command: XOCommand<XOSetRequestData>,
        override val data: XOSetRequestData
): XORequest<XOSetRequest.XOSetRequestData>{
    data class XOSetRequestData(
            val num: Int
    )
}

interface XORequest <T>{
    val command: XOCommand<T>
    val data: T
}

class XOCommand<T>(val clazz: Class<out XORequest<T>>){
    companion object{
        val SET = XOCommand(XOSetRequest::class.java)

        val values = mapOf("set" to SET)
        fun valueOf(command: String) = values[command.toLowerCase()]
    }
}