package com.tangem.blockchain.common

interface ExceptionHandlerOutput {
    fun handleApiSwitch(currentHost: String, nextHost: String, message: String)
}

object ExceptionHandler {
    private val outputs: MutableList<ExceptionHandlerOutput> = mutableListOf()

    fun append(output: ExceptionHandlerOutput) {
        outputs.add(output)
    }

    internal fun handleApiSwitch(currentHost: String, nextHost: String, message: String) {
        outputs.forEach {
            it.handleApiSwitch(currentHost, nextHost, message)
        }
    }
}