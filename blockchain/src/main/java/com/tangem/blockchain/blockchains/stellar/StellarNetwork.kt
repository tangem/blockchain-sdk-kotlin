package com.tangem.blockchain.blockchains.stellar

sealed class StellarNetwork(val url: String) {

    data class Nownodes(val apiKey: String) : StellarNetwork(url = "https://xlm.nownodes.io/$apiKey/")

    data class Public(val baseUrl: String) : StellarNetwork(url = baseUrl)

    object HorizonTestnet : StellarNetwork(url = "https://horizon-testnet.stellar.org/")
}