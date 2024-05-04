package com.tangem.blockchain.blockchains.koinos.network

internal interface KoinContractOperation {
    val entryPoint: Int
    val name: String
    val argsName: String
    val resultName: String
    val eventName: String get() = ""
}