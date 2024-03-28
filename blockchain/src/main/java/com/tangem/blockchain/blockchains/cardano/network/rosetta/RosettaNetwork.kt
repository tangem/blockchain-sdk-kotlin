package com.tangem.blockchain.blockchains.cardano.network.rosetta

sealed class RosettaNetwork(val url: String) {

    data class Getblock(val accessToken: String) : RosettaNetwork(url = "https://go.getblock.io/$accessToken/")

    object Tangem : RosettaNetwork(url = "https://ada.tangem.com/")
}