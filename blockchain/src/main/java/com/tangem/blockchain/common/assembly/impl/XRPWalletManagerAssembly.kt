package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.xrp.XRPProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object XRPWalletManagerAssembly : WalletManagerAssembly<XrpWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): XrpWalletManager {
        return with(input.wallet) {
            val networkService = XrpNetworkService(
                providers = XRPProvidersBuilder(input.providerTypes, input.config).build(blockchain),
            )

            XrpWalletManager(
                wallet = this,
                transactionBuilder = XrpTransactionBuilder(networkService, publicKey.blockchainKey),
                networkProvider = networkService,
            )
        }
    }
}