package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cosmos.CosmosWalletManager
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank

internal object TerraV1WalletManagerAssembly : WalletManagerAssembly<CosmosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CosmosWalletManager {
        val providers = buildList {
            input.config.nowNodeCredentials?.apiKey.letNotBlank { add("https://lunc.nownodes.io/$it/") }
            add("https://terra-classic-lcd.publicnode.com/")
        }.map(::CosmosRestProvider)

        return CosmosWalletManager(
            wallet = input.wallet,
            networkProviders = providers,
            cosmosChain = CosmosChain.TerraV1,
        )
    }
}