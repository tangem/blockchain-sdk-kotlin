package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.tron.TronTransactionBuilder
import com.tangem.blockchain.blockchains.tron.TronWalletManager
import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetwork
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank

internal object TronWalletManagerAssembly : WalletManagerAssembly<TronWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TronWalletManager {
        with(input.wallet) {
            val networks = if (!blockchain.isTestnet()) {
                buildList {
                    add(TronNetwork.TronGrid(null))
                    input.config.tronGridApiKey.letNotBlank {
                        add(TronNetwork.TronGrid(it))
                    }
                    input.config.nowNodeCredentials?.apiKey.letNotBlank {
                        add(TronNetwork.NowNodes(it))
                    }
                    input.config.getBlockCredentials?.apiKey.letNotBlank {
                        add(TronNetwork.GetBlock(it))
                    }
                }
            } else {
                listOf<TronNetwork>(TronNetwork.Nile)
            }
            val rpcProviders = networks.map {
                TronJsonRpcNetworkProvider(network = it)
            }

            return TronWalletManager(
                wallet = this,
                transactionBuilder = TronTransactionBuilder(blockchain),
                networkService = TronNetworkService(
                    rpcNetworkProviders = rpcProviders,
                    blockchain = input.wallet.blockchain,
                )
            )
        }
    }

}