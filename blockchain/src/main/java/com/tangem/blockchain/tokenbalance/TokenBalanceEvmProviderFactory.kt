package com.tangem.blockchain.tokenbalance

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.tokenbalance.providers.moralis.evm.MoralisEvmTokenBalanceProvider

internal object TokenBalanceEvmProviderFactory {

    fun createTokenBalanceEvmProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TokenBalanceProvider =
        when {
            blockchain.canHandleTokenBalances() -> MoralisEvmTokenBalanceProvider(
                blockchain = blockchain,
                apiKey = config.moralisApiKey,
            )
            else -> DefaultTokenBalanceProvider
        }
}