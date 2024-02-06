package com.tangem.blockchain.blockchains.cosmos.network

import androidx.annotation.VisibleForTesting
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeeSelectionState
import wallet.core.jni.CoinType
import java.math.BigDecimal

sealed interface CosmosChain {
    val smallestDenomination: String
    val blockchain: Blockchain
    val chainId: String

    // Often times the value specified in Keplr is not enough:
    // >>> out of gas in location: WriteFlat; gasWanted: 76012, gasUsed: 76391: out of gas
    // >>> out of gas in location: ReadFlat; gasWanted: 124626, gasUsed: 125279: out of gas
    val gasMultiplier: BigDecimal
    // We use a formula to calculate the fee, by multiplying estimated gas by gas price.
    // But sometimes this is not enough:
    // >>> insufficient fees; got: 1005uluna required: 1006uluna: insufficient fee
    // Default multiplier value is 1
    val feeMultiplier: BigDecimal
    val tokenDenominationByContractAddress: Map<String, String> get() = emptyMap()
    val taxPercentByContractAddress: Map<String, BigDecimal> get() = emptyMap()
    val coin: CoinType
    val allowsFeeSelection: FeeSelectionState get() = FeeSelectionState.Unspecified

    fun gasPrices(amountType: AmountType): List<BigDecimal>
    fun getExtraFee(amount: Amount): BigDecimal? = null

    data class Cosmos(val testnet: Boolean) : CosmosChain {
        override val blockchain: Blockchain = if (testnet) Blockchain.CosmosTestnet else Blockchain.Cosmos
        override val chainId: String = if (testnet) "theta-testnet-001" else "cosmoshub-4"
        override val smallestDenomination: String = "uatom"
        override val gasMultiplier: BigDecimal = BigDecimal(2)
        override val feeMultiplier: BigDecimal = BigDecimal(1.0)
        override val coin: CoinType = CoinType.COSMOS

        @Suppress("MagicNumber")
        override fun gasPrices(amountType: AmountType): List<BigDecimal> = listOf(
            BigDecimal(0.01),
            BigDecimal(0.025),
            BigDecimal(0.03),
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Suppress("MagicNumber")
    object Gaia : CosmosChain {
        override val blockchain: Blockchain = Blockchain.CosmosTestnet
        override val chainId: String = "gaia-13003"
        override val smallestDenomination: String = "muon"
        override val gasMultiplier: BigDecimal = BigDecimal(2)
        override val feeMultiplier: BigDecimal = BigDecimal(1.0)
        override val coin: CoinType = CoinType.COSMOS
        override fun gasPrices(amountType: AmountType): List<BigDecimal> = error("Not implemented")
    }

    @Suppress("MagicNumber")
    object TerraV1 : CosmosChain {
        override val blockchain: Blockchain = Blockchain.TerraV1
        override val chainId: String = "columbus-5"
        override val smallestDenomination: String = "uluna"
        override val gasMultiplier: BigDecimal = BigDecimal(4)
        override val feeMultiplier: BigDecimal = BigDecimal(1.5)
        override val coin: CoinType = CoinType.TERRA
        override val allowsFeeSelection: FeeSelectionState = FeeSelectionState.Forbids
        override val tokenDenominationByContractAddress: Map<String, String> = mapOf("uusd" to "uusd")
        override val taxPercentByContractAddress: Map<String, BigDecimal> = mapOf("uusd" to BigDecimal("0.2"))

        // Stability or "spread" fee. Applied to both main currency and tokens
        // https://classic-docs.terra.money/docs/learn/fees.html#spread-fee
        private val minimumSpreadFeePercentage = BigDecimal("0.005") // 0.5%

        override fun gasPrices(amountType: AmountType): List<BigDecimal> = when (amountType) {
            AmountType.Coin -> listOf(BigDecimal(28.325))
            else -> listOf(BigDecimal(1.0))
        }

        override fun getExtraFee(amount: Amount): BigDecimal? {
            return amount.value?.times(minimumSpreadFeePercentage)
        }
    }

    @Suppress("MagicNumber")
    object TerraV2 : CosmosChain {
        override val blockchain: Blockchain = Blockchain.TerraV2
        override val chainId: String = "phoenix-1"
        override val smallestDenomination: String = "uluna"
        override val gasMultiplier: BigDecimal = BigDecimal(2)
        override val feeMultiplier: BigDecimal = BigDecimal(1.5)
        override val coin: CoinType = CoinType.TERRAV2
        override fun gasPrices(amountType: AmountType): List<BigDecimal> = listOf(
            BigDecimal(0.015),
            BigDecimal(0.025),
            BigDecimal(0.040),
        )
    }
}
