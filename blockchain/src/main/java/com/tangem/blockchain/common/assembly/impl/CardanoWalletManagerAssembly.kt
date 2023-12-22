package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.cardano.network.RosettaNetwork
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetworkProvider
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_ADALITE

internal object CardanoWalletManagerAssembly : WalletManagerAssembly<CardanoWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CardanoWalletManager {
        with(input.wallet) {
            val providers = buildList {
                input.config.getBlockCredentials?.cardano?.rosetta.letNotBlank {
                    add(RosettaNetworkProvider(RosettaNetwork.RosettaGetblock(it)))
                }
                add(AdaliteNetworkProvider(API_ADALITE))
                add(RosettaNetworkProvider(RosettaNetwork.RosettaTangem))
            }

            return CardanoWalletManager(
                this,
                CardanoTransactionBuilder(),
                CardanoNetworkService(providers),
            )
        }
    }
}
