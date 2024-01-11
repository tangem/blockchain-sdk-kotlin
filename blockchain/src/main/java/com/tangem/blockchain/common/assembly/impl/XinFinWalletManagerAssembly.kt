package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.decimal.DecimalNetworkService
import com.tangem.blockchain.blockchains.decimal.DecimalWalletManager
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.xdc.XinFinWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object XinFinWalletManagerAssembly : WalletManagerAssembly<XinFinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): XinFinWalletManager {
        return with(input.wallet) {
            XinFinWalletManager(
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