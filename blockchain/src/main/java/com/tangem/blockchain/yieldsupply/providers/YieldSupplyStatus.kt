package com.tangem.blockchain.yieldsupply.providers

import java.math.BigDecimal

/**
 *  Information about the status of the yield token
 *
 *  @property isInitialized     Indicates whether the yield token is initialized
 *  @property isActive          Indicates whether the yield token is active
 *  @property maxNetworkFee     The maximum network fee associated with the yield token
 */
data class YieldSupplyStatus(
    val isInitialized: Boolean,
    val isActive: Boolean,
    val maxNetworkFee: BigDecimal,
)