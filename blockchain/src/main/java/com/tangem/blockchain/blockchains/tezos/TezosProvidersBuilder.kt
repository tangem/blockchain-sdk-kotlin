package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.network.API_TEZOS_BLOCKSCALE
import com.tangem.blockchain.network.API_TEZOS_ECAD
import com.tangem.blockchain.network.API_TEZOS_SMARTPY

internal object TezosProvidersBuilder : NetworkProvidersBuilder<TezosNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Tezos)

    override fun createProviders(blockchain: Blockchain): List<TezosNetworkProvider> {
        return listOf(API_TEZOS_BLOCKSCALE, API_TEZOS_SMARTPY, API_TEZOS_ECAD)
            .map(::TezosJsonRpcNetworkProvider)
    }
}