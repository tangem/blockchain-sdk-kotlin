package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.icp.ICPProvidersBuilder
import com.tangem.blockchain.blockchains.icp.ICPWalletManager
import com.tangem.blockchain.blockchains.icp.ICPTransactionBuilder
import com.tangem.blockchain.blockchains.icp.network.ICPNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.ProviderType

internal object ICPWalletManagerAssembly : WalletManagerAssembly<ICPWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): ICPWalletManager {
        return with(input.wallet) {
            ICPWalletManager(
                wallet = this,
                networkProvider = ICPNetworkService(
                    ICPProvidersBuilder(
                        listOf(ProviderType.Public("https://icp-api.io/")),
                        publicKey
                    ).build(blockchain)
                ),
                transactionBuilder = ICPTransactionBuilder(blockchain)
            )
        }
    }
}
