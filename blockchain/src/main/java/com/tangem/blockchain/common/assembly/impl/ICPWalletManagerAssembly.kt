package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.icp.ICPProvidersBuilder
import com.tangem.blockchain.blockchains.icp.ICPTransactionBuilder
import com.tangem.blockchain.blockchains.icp.ICPWalletManager
import com.tangem.blockchain.blockchains.icp.network.ICPNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object ICPWalletManagerAssembly : WalletManagerAssembly<ICPWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): ICPWalletManager {
        return with(input.wallet) {
            ICPWalletManager(
                wallet = this,
                networkProvider = ICPNetworkService(
                    ICPProvidersBuilder(
                        providerTypes = input.providerTypes,
                        walletPublicKey = publicKey,
                    ).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionBuilder = ICPTransactionBuilder(blockchain),
            )
        }
    }
}