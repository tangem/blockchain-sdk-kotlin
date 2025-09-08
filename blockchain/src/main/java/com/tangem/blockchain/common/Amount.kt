package com.tangem.blockchain.common

import java.math.BigDecimal
import java.math.RoundingMode
import com.tangem.blockchain.common.Token as BlockchainToken

data class Amount(
    val currencySymbol: String,
    val value: BigDecimal? = null,
    val maxValue: BigDecimal? = null,
    val decimals: Int,
    val type: AmountType = AmountType.Coin,
) {

    /**
     * Get value as long
     *
     * IMPORTANT!
     * If the value after the { movePointRight(decimals) } operation has a fractional part, the result will be 0.
     *
     * @see {AmountTests}
     */
    val longValue: Long
        get() = requireNotNull(value = value, lazyMessage = { "Amount value is null" })
            .movePointRight(decimals)
            .setScale(0, RoundingMode.DOWN) // remove fractional part
            .longValueExact()

    constructor(
        value: BigDecimal?,
        blockchain: Blockchain,
        type: AmountType = AmountType.Coin,
        currencySymbol: String? = null,
        maxValue: BigDecimal? = null,
    ) : this(
        currencySymbol = currencySymbol ?: blockchain.currency,
        value = value,
        maxValue = maxValue,
        decimals = blockchain.decimals(),
        type = type,
    )

    constructor(token: BlockchainToken, value: BigDecimal? = null) : this(
        currencySymbol = token.symbol,
        value = value,
        maxValue = null,
        decimals = token.decimals,
        type = AmountType.Token(token),
    )

    constructor(amount: Amount, value: BigDecimal) : this(
        currencySymbol = amount.currencySymbol,
        value = value,
        maxValue = null,
        decimals = amount.decimals,
        type = amount.type,
    )

    constructor(blockchain: Blockchain) : this(
        currencySymbol = blockchain.currency,
        value = BigDecimal.ZERO,
        maxValue = null,
        decimals = blockchain.decimals(),
        type = AmountType.Coin,
    )

    operator fun plus(add: BigDecimal): Amount = copy(value = (value ?: BigDecimal.ZERO).plus(add))

    operator fun minus(extract: BigDecimal): Amount = copy(value = (value ?: BigDecimal.ZERO).minus(extract))
}

sealed class AmountType {
    object Coin : AmountType()
    object Reserve : AmountType()
    data class FeeResource(val name: String? = null) : AmountType()
    data class Token(val token: BlockchainToken) : AmountType()
    data class YieldLend(
        val token: BlockchainToken,
        val isActive: Boolean,
        val isInitialized: Boolean,
        val isAllowedToSpend: Boolean,
    ) : AmountType()
}