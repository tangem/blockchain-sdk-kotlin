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

    private fun Blockchain.canHandleTokenBalances(): Boolean = when (this) {
        Blockchain.Ethereum, // supported testnet - Sepolia (11155111)
        Blockchain.Arbitrum,
        Blockchain.Avalanche,
        Blockchain.Fantom,
        Blockchain.BSC, Blockchain.BSCTestnet,
        Blockchain.Polygon, // supported testnet - Amoy (80002)
        Blockchain.Cronos,
        Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
        Blockchain.Moonriver,
        Blockchain.Chiliz, Blockchain.ChilizTestnet,
        Blockchain.Optimism,
        Blockchain.Base, Blockchain.BaseTestnet,
        Blockchain.Gnosis, // supported testnet - Chiado (10200)
        Blockchain.Linea, Blockchain.LineaTestnet,
        Blockchain.PulseChain,
        Blockchain.Monad,
        -> true
        else -> false
    }
}