package com.tangem.blockchain.blockchains.stellar

sealed class StellarNetwork(val url: String) {

    data class Nownodes(val apiKey: String) : StellarNetwork(url = "https://xlm.nownodes.io/$apiKey/")

    data class QuickNode(val subdomain: String, val apiKey: String) :
        StellarNetwork(url = "https://$subdomain/$apiKey/")

    data class GetBlock(val accessToken: String) :
        StellarNetwork(url = "https://xlm.getblock.io/$accessToken/mainnet/")

    data class Public(val baseUrl: String) : StellarNetwork(url = baseUrl)

    object HorizonTestnet : StellarNetwork(url = "https://horizon-testnet.stellar.org/")
}