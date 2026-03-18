package com.tangem.blockchain.tokenbalance

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.tokenbalance.providers.moralis.evm.MoralisEvmTokenBalanceProvider

internal object TokenBalanceProviderFactory {

    fun createTokenBalanceProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TokenBalanceProvider = when {
        blockchain.canHandleTokenBalances() -> {
            when {
                blockchain.isEvm() -> MoralisEvmTokenBalanceProvider(
                    blockchain = blockchain,
                    apiKey = config.moralisApiKey,
                )
                else -> DefaultTokenBalanceProvider
            }
        }
        else -> DefaultTokenBalanceProvider
    }
}