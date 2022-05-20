package com.tangem.blockchain.blockchains.tron.network

enum class TronNetwork(val url: String) {
    MAINNET(url = "https://api.trongrid.io"),
    SHASTA(url = "https://api.shasta.trongrid.io/"),
    NILE(url = "https://nile.trongrid.io")
}