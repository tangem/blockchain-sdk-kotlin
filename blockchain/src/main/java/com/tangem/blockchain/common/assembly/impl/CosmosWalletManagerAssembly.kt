package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cosmos.CosmosProvidersBuilder
import com.tangem.blockchain.blockchains.cosmos.CosmosWalletManager
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object CosmosWalletManagerAssembly : WalletManagerAssembly<CosmosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CosmosWalletManager {
        return with(input.wallet) {
            val testnet = blockchain.isTestnet()

            CosmosWalletManager(
                wallet = input.wallet,
                networkProviders = CosmosProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                cosmosChain = CosmosChain.Cosmos(testnet),
            )
        }
    }
}