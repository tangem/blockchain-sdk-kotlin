package com.tangem.blockchain.blockchains.cosmos.network

import androidx.annotation.VisibleForTesting
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeeSelectionState
import wallet.core.jni.CoinType
import java.math.BigDecimal

sealed interface CosmosChain {
    val smallestDenomination: String
    val blockchain: Blockchain
    val chainId: String
    fun gasPrices(amountType: AmountType): List<Double>

    // Often times the value specified in Keplr is not enough:
    // >>> out of gas in location: WriteFlat; gasWanted: 76012, gasUsed: 76391: out of gas
    // >>> out of gas in location: ReadFlat; gasWanted: 124626, gasUsed: 125279: out of gas
    val gasMultiplier: Long
    // We use a formula to calculate the fee, by multiplying estimated gas by gas price.
    // But sometimes this is not enough:
    // >>> insufficient fees; got: 1005uluna required: 1006uluna: insufficient fee
    // Default multiplier value is 1
    val feeMultiplier: Double
    val tokenDenominationByContractAddress: Map<String, String> get() = emptyMap()
    val taxPercentByContractAddress : Map<String, BigDecimal> get() = emptyMap()
    val coin: CoinType
    val allowsFeeSelection: FeeSelectionState get() = FeeSelectionState.Unspecified

    data class Cosmos(val testnet: Boolean) : CosmosChain {
        override val blockchain: Blockchain = if (testnet) Blockchain.CosmosTestnet else Blockchain.Cosmos
        override val chainId: String = if (testnet) "theta-testnet-001" else "cosmoshub-4"
        override fun gasPrices(amountType: AmountType): List<Double> = listOf(0.01, 0.025, 0.03)
        override val smallestDenomination: String = "uatom"
        override val gasMultiplier: Long = 2
        override val feeMultiplier: Double = 1.0
        override val coin: CoinType = CoinType.COSMOS
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    object Gaia : CosmosChain {
        override val blockchain: Blockchain = Blockchain.CosmosTestnet
        override val chainId: String = "gaia-13003"
        override fun gasPrices(amountType: AmountType): List<Double> = throw IllegalStateException()
        override val smallestDenomination: String = "muon"
        override val gasMultiplier: Long = 2
        override val feeMultiplier: Double = 1.0
        override val coin: CoinType = CoinType.COSMOS
    }

    object TerraV1 : CosmosChain {
        override val blockchain: Blockchain = Blockchain.TerraV1
        override val chainId: String = "columbus-5"
        override val smallestDenomination: String = "uluna"
        override val gasMultiplier: Long = 4
        override val feeMultiplier: Double = 1.5
        override val coin: CoinType = CoinType.TERRA
        override val allowsFeeSelection: FeeSelectionState = FeeSelectionState.Forbids
        override val tokenDenominationByContractAddress: Map<String, String> = mapOf("uusd" to "uusd")
        override val taxPercentByContractAddress: Map<String, BigDecimal> = mapOf("uusd" to BigDecimal("0.2"))
        override fun gasPrices(amountType: AmountType): List<Double> = when (amountType) {
            AmountType.Coin -> listOf(28.325)
            else -> listOf(1.0)
        }
    }

    object TerraV2 : CosmosChain {
        override val blockchain: Blockchain = Blockchain.TerraV2
        override val chainId: String = "phoenix-1"
        override val smallestDenomination: String = "uluna"
        override val gasMultiplier: Long = 2
        override val feeMultiplier: Double = 1.5
        override val coin: CoinType = CoinType.TERRAV2
        override fun gasPrices(amountType: AmountType): List<Double> = listOf(0.015, 0.025, 0.040)
    }
}