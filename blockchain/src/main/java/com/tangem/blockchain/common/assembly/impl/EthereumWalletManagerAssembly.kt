package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionHistoryProvider
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider

internal object EthereumWalletManagerAssembly : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain
                ),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(input.config),
                    blockcypherNetworkProvider = BlockcypherNetworkProvider(
                        blockchain = blockchain,
                        tokens = input.config.blockcypherTokens
                    ),
                ),
                transactionHistoryProvider = when (blockchain) {
                    Blockchain.Ethereum -> {
                        if (input.config.nowNodeCredentials != null && input.config.nowNodeCredentials.apiKey.isNotBlank()) {
                            EthereumTransactionHistoryProvider(
                                blockchain = blockchain,
                                blockBookApi = BlockBookApi(
                                    config = BlockBookConfig.NowNodes(nowNodesCredentials = input.config.nowNodeCredentials),
                                    blockchain = blockchain,
                                )
                            )
                        } else {
                            DefaultTransactionHistoryProvider
                        }
                    }

                    else -> DefaultTransactionHistoryProvider
                }
            )
        }
    }
}
