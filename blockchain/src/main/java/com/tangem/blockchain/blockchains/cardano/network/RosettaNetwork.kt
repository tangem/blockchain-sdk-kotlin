package com.tangem.blockchain.blockchains.cardano.network

sealed class RosettaNetwork(val url: String) {

    data class RosettaGetblock(val apiKey: String) : RosettaNetwork(url = "https://ada.getblock.io/mainnet/${apiKey}/")
    object RosettaTangem : RosettaNetwork(url = "https://ada.tangem.com/")
}