package com.tangem.blockchain.common

import java.math.BigDecimal
import com.tangem.blockchain.common.Token as BlockchainToken

data class Amount(
    val currencySymbol: String,
    val value: BigDecimal? = null,
    val maxValue: BigDecimal? = null,
    val decimals: Int,
    val type: AmountType = AmountType.Coin,
) {

    val longValue get() = value?.movePointRight(decimals)?.toLong()

    val longValueOrZero: Long by lazy { value?.movePointRight(decimals)?.toLong() ?: 0L }

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

    constructor(token: BlockchainToken, value: BigDecimal? = null) :
        this(
            currencySymbol = token.symbol,
            value = value,
            maxValue = null,
            decimals = token.decimals,
            type = AmountType.Token(token),
        )

    constructor(amount: Amount, value: BigDecimal) :
        this(
            currencySymbol = amount.currencySymbol,
            value = value,
            maxValue = null,
            decimals = amount.decimals,
            type = amount.type,
        )

    constructor(blockchain: Blockchain) :
        this(
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

    data class FeeResource(val name: String? = null) : AmountType() {
        companion object : AmountType()
    }

    data class Token(val token: BlockchainToken) : AmountType()
}