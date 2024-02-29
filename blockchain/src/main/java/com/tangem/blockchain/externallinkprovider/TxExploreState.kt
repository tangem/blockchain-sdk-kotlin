package com.tangem.blockchain.externallinkprovider

sealed class TxExploreState {

    data class Url(val url: String) : TxExploreState()

    object Unsupported : TxExploreState()
}