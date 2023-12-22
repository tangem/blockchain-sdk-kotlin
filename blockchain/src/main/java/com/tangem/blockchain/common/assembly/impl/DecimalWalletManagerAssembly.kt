package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.decimal.DecimalNetworkService
import com.tangem.blockchain.blockchains.decimal.DecimalWalletManager
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object DecimalWalletManagerAssembly : WalletManagerAssembly<DecimalWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DecimalWalletManager {
        return with(input.wallet) {
            DecimalWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = DecimalNetworkService(
                    jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(input.config),
                ),
            )
        }
    }
}
