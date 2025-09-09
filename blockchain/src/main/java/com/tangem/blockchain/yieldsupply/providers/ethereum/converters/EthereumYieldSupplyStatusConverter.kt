package com.tangem.blockchain.yieldsupply.providers.ethereum.converters

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.WORD_HEX_LENGTH
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus

/**
 * Converts the result of the `getYieldSupplyStatus` call to [YieldSupplyStatus].
 *
 * @param decimals The number of decimal
 */
@Suppress("MagicNumber")
internal class EthereumYieldSupplyStatusConverter(
    private val decimals: Int,
) {

    /**
     * Converts the [result] of the `getYieldSupplyStatus` call to [YieldSupplyStatus].
     *
     * 0x0000000000000000000000000000000000000000000000000000000000000001 -> true
     * * 0000000000000000000000000000000000000000000000000000000000000001 -> true
     * * 0000000000000000000000000000000000000000000000008ac7230489e80000 -> 10.0
     */
    fun convert(result: String): YieldSupplyStatus {
        val clean = result.removePrefix(HEX_PREFIX)

        val isInitializedHex = clean.substring(0, WORD_HEX_LENGTH)
        val isActiveHex = clean.substring(WORD_HEX_LENGTH, 2 * WORD_HEX_LENGTH)
        val maxNetworkFeeHex = clean.substring(2 * WORD_HEX_LENGTH, 3 * WORD_HEX_LENGTH)

        val isInitial = isInitializedHex.endsWith("1")
        val isActive = isActiveHex.endsWith("1")
        val maxNetworkFee = maxNetworkFeeHex.toBigInteger(16).toBigDecimal().movePointLeft(decimals)

        return YieldSupplyStatus(
            isInitialized = isInitial,
            isActive = isActive,
            maxNetworkFee = maxNetworkFee,
        )
    }
}