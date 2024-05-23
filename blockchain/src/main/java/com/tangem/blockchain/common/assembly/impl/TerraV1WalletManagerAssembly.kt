package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cosmos.CosmosWalletManager
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.terra.TerraV1ProvidersBuilder
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object TerraV1WalletManagerAssembly : WalletManagerAssembly<CosmosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CosmosWalletManager {
        return with(input) {
            CosmosWalletManager(
                wallet = wallet,
                networkProviders = TerraV1ProvidersBuilder(input.providerTypes, config).build(wallet.blockchain),
                cosmosChain = CosmosChain.TerraV1,
            )
        }
    }
}