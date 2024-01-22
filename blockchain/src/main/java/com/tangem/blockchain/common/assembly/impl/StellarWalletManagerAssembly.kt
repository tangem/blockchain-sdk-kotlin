package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.stellar.StellarNetwork
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank

internal object StellarWalletManagerAssembly : WalletManagerAssembly<StellarWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): StellarWalletManager {
        with(input.wallet) {
            val isTestnet = blockchain == Blockchain.StellarTestnet
            val hosts = if (!isTestnet) {
                buildList {
                    add(StellarNetwork.Horizon)
                    input.config.nowNodeCredentials?.apiKey.letNotBlank { add(StellarNetwork.Nownodes(it)) }
                }
            } else {
                listOf<StellarNetwork>(StellarNetwork.HorizonTestnet)
            }
            val networkService = StellarNetworkService(
                hosts = hosts,
                isTestnet = isTestnet,
            )
            return StellarWalletManager(
                this,
                StellarTransactionBuilder(networkService, publicKey.blockchainKey),
                networkService,
            )
        }
    }
}
