package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.xrp.XRPProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage

internal class XRPWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
) : WalletManagerAssembly<XrpWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): XrpWalletManager {
        return with(input.wallet) {
            val networkService = XrpNetworkService(
                providers = XRPProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                blockchain = blockchain,
            )

            XrpWalletManager(
                wallet = this,
                transactionBuilder = XrpTransactionBuilder(networkService, publicKey.blockchainKey),
                networkProvider = networkService,
                dataStorage = dataStorage,
            )
        }
    }
}