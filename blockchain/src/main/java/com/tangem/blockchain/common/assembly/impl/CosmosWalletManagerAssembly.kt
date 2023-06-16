package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cosmos.CosmosWalletManager
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank

internal object CosmosWalletManagerAssembly : WalletManagerAssembly<CosmosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CosmosWalletManager {
        with(input.wallet) {
            val testnet = blockchain.isTestnet()
            val providers = buildList {
                if (testnet) {
                    add("https://rest.seed-01.theta-testnet.polypore.xyz")
                } else {
                    input.config.nowNodeCredentials?.apiKey.letNotBlank { add("https://atom.nownodes.io/$it/") }
                    input.config.getBlockCredentials?.apiKey.letNotBlank { add("https://atom.getblock.io/$it/") }

                    add("https://cosmos-mainnet-rpc.allthatnode.com:1317/")
                    // This is a REST proxy combining the servers below (and others)
                    add("https://rest.cosmos.directory/cosmoshub/")
                    add("https://cosmoshub-api.lavenderfive.com/")
                    add("https://rest-cosmoshub.ecostake.com/")
                    add("https://lcd.cosmos.dragonstake.io/")
                }
            }.map(::CosmosRestProvider)

            return CosmosWalletManager(
                wallet = input.wallet,
                networkProviders = providers,
                cosmosChain = CosmosChain.Cosmos(testnet),
                )
        }
    }

}