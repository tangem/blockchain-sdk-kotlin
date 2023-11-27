package com.tangem.blockchain.blockchains.cardano.network

sealed class RosettaNetwork(val url: String) {

    data class RosettaGetblock(val accessToken: String) : RosettaNetwork(url = "https://go.getblock.io/$accessToken/")
    object RosettaTangem : RosettaNetwork(url = "https://ada.tangem.com/")
}