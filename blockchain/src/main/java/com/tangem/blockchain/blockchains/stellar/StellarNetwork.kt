package com.tangem.blockchain.blockchains.stellar

sealed class StellarNetwork(val url: String) {

    data class Nownodes(val apiKey: String) : StellarNetwork(url = "https://xlm.nownodes.io/$apiKey/")
    data class Getblock(val accessToken: String) : StellarNetwork(url = "https://go.getblock.io/$accessToken/")
    object Horizon : StellarNetwork(url = "https://horizon.stellar.org/")
    object HorizonTestnet : StellarNetwork(url = "https://horizon-testnet.stellar.org/")
}