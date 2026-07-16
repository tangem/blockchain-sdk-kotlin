package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cosmos.CosmosWalletManager
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.gonka.GonkaProvidersBuilder
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object GonkaWalletManagerAssembly : WalletManagerAssembly<CosmosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CosmosWalletManager {
        return CosmosWalletManager(
            wallet = input.wallet,
            networkProviders = GonkaProvidersBuilder(input.providerTypes).build(input.wallet.blockchain),
            cosmosChain = CosmosChain.Gonka,
            blockchain = input.wallet.blockchain,
        )
    }
}