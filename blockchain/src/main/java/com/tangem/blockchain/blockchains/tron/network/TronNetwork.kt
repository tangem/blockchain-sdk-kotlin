package com.tangem.blockchain.blockchains.tron.network

sealed class TronNetwork(val url: String) {

    data class NowNodes(val apiKey: String) : TronNetwork(url = "https://trx.nownodes.io/")

    data class GetBlock(val accessToken: String) : TronNetwork(url = "https://go.getblock.io/$accessToken/")

    data class PublicTronGrid(val baseUrl: String) : TronNetwork(url = baseUrl)

    data class TronGrid(val apiKey: String) : TronNetwork(url = "https://api.trongrid.io/")

    object Nile : TronNetwork(url = "https://nile.trongrid.io/")
}