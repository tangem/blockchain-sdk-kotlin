package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider

internal object EthereumWalletManagerAssembly : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(input.config),
                    blockcypherNetworkProvider = BlockcypherNetworkProvider(
                        blockchain = blockchain,
                        tokens = input.config.blockcypherTokens,
                    ),
                ),
                transactionHistoryProvider = blockchain.getTransactionHistoryProvider(input.config),
            )
        }
    }
}
