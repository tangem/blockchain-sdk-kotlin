package com.tangem.blockchain.blockchains.cosmos.network

import androidx.annotation.VisibleForTesting
import com.tangem.blockchain.common.Blockchain

sealed interface CosmosChain {
    val smallestDenomination: String
    val blockchain: Blockchain
    val chainId: String
    val gasPrices: List<Double>

    data class Cosmos(
        val testnet: Boolean,
    ) : CosmosChain {
        override val blockchain: Blockchain = if (testnet) Blockchain.CosmosTestnet else Blockchain.Cosmos
        override val chainId: String = if (testnet) "theta-testnet-001" else "cosmoshub-4"
        override val gasPrices: List<Double> = listOf(0.01, 0.025, 0.03)
        override val smallestDenomination: String = "uatom"
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    object Gaia : CosmosChain {
        override val blockchain: Blockchain = Blockchain.CosmosTestnet
        override val chainId: String = "gaia-13003"
        override val gasPrices: List<Double> = throw IllegalStateException()
        override val smallestDenomination: String = "muon"
    }
}