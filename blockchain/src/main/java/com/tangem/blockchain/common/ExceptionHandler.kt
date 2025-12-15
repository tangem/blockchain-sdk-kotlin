package com.tangem.blockchain.common

interface ExceptionHandlerOutput {
    fun handleApiSwitch(currentHost: String, nextHost: String, message: String, blockchain: Blockchain)
}

object ExceptionHandler {
    private val outputs: MutableList<ExceptionHandlerOutput> = mutableListOf()

    fun append(output: ExceptionHandlerOutput) {
        outputs.add(output)
    }

    internal fun handleApiSwitch(currentHost: String, nextHost: String, message: String, blockchain: Blockchain) {
        outputs.forEach {
            it.handleApiSwitch(currentHost, nextHost, message, blockchain)
        }
    }
}