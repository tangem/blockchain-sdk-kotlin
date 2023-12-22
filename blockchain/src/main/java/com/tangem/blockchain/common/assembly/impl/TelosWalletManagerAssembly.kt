package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.telos.TelosWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object TelosWalletManagerAssembly : WalletManagerAssembly<TelosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TelosWalletManager {
        with(input.wallet) {
            return TelosWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(input.config),
                ),
            )
        }
    }
}
