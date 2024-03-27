package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.nexa.NexaTransactionBuilder
import com.tangem.blockchain.blockchains.nexa.NexaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.electrum.ElectrumMultiNetworkProvider
import com.tangem.blockchain.network.electrum.getElectrumNetworkProviders

internal object NexaWalletManagerAssembly : WalletManagerAssembly<NexaWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): NexaWalletManager {
        with(input.wallet) {
            return NexaWalletManager(
                wallet = this,
                networkProvider = ElectrumMultiNetworkProvider(
                    providers = blockchain.getElectrumNetworkProviders(),
                ),
                transactionBuilder = NexaTransactionBuilder(
                    walletPublicKey = byteArrayOf(0x02) + publicKey.blockchainKey,
                    blockchain = blockchain
                )
            )
        }
    }
}