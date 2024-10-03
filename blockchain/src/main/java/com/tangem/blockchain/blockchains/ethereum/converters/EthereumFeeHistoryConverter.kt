package com.tangem.blockchain.blockchains.ethereum.converters

import com.tangem.blockchain.blockchains.ethereum.models.EthereumFeeHistoryResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumFeeHistory
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Converter from [EthereumFeeHistoryResponse] to [EthereumFeeHistory]
 *
[REDACTED_AUTHOR]
 */
internal object EthereumFeeHistoryConverter {

    fun convert(response: EthereumFeeHistoryResponse): EthereumFeeHistory {
        if (response.baseFeePerGas.isEmpty() || response.reward.isEmpty()) {
            error("baseFeePerGas and reward are empty")
        }

        val pendingBaseFeeString = response.baseFeePerGas.lastOrNull()

        if (pendingBaseFeeString == null || pendingBaseFeeString == "0x0") {
            error("pendingBaseFeeString is null or 0x0")
        }

        val lowRewards = response.reward.mapNotNull { it.getOrNull(0) }
        val marketRewards = response.reward.mapNotNull { it.getOrNull(1) }
        val fastRewards = response.reward.mapNotNull { it.getOrNull(2) }

        return EthereumFeeHistory.Common(
            baseFee = pendingBaseFeeString.toBigDecimal(),
            lowPriorityFee = getAverageReward(lowRewards),
            marketPriorityFee = getAverageReward(marketRewards),
            fastPriorityFee = getAverageReward(fastRewards),
        )
    }

    private fun getAverageReward(rewards: List<String>): BigDecimal {
        val filteredRewards = rewards.filter { it != "0x0" }

        if (filteredRewards.isEmpty()) error("filteredRewards is empty")

        val sum = filteredRewards
            .map { it.toBigDecimal() }
            .reduce { acc, decimal -> acc + decimal }

        val total = BigDecimal(filteredRewards.size)

        val averageDecimal = sum.divide(total, MathContext.DECIMAL128)
            .setScale(0, RoundingMode.HALF_UP)

        if (averageDecimal <= BigDecimal.ZERO) error("averageDecimal [$averageDecimal] is negative")

        return averageDecimal
    }

    private fun String.toBigDecimal(): BigDecimal {
        val value = removePrefix(prefix = "0x").toLongOrNull(radix = 16)

        return BigDecimal(
            requireNotNull(value) { "Failed to convert from $this to BigDecimal" },
        )
    }
}