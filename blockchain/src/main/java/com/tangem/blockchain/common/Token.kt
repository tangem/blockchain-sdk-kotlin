package com.tangem.blockchain.common

data class Token(
        val name: String,
        val symbol: String,
        val contractAddress: String,
        val decimals: Int
) {
    constructor(symbol: String, contractAddress: String, decimals: Int) :
            this(symbol, symbol, contractAddress, decimals)
}