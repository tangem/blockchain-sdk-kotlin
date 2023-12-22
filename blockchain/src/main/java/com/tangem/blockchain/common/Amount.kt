package com.tangem.blockchain.common

import java.math.BigDecimal
import com.tangem.blockchain.common.Token as BlockchainToken

data class Amount(
    val currencySymbol: String,
    val value: BigDecimal? = null,
    val decimals: Int,
    val type: AmountType = AmountType.Coin,
) {

    val longValue get() = value?.movePointRight(decimals)?.toLong()

    constructor(
        value: BigDecimal?,
        blockchain: Blockchain,
        type: AmountType = AmountType.Coin,
    ) : this(blockchain.currency, value, blockchain.decimals(), type)

    constructor(token: BlockchainToken, value: BigDecimal? = null) :
        this(token.symbol, value, token.decimals, AmountType.Token(token))

    constructor(amount: Amount, value: BigDecimal) :
        this(amount.currencySymbol, value, amount.decimals, amount.type)

    constructor(blockchain: Blockchain) :
        this(blockchain.currency, BigDecimal.ZERO, blockchain.decimals(), AmountType.Coin)

    operator fun plus(add: BigDecimal): Amount = copy(value = (value ?: BigDecimal.ZERO).plus(add))

    operator fun minus(extract: BigDecimal): Amount = copy(value = (value ?: BigDecimal.ZERO).minus(extract))
}

sealed class AmountType {
    object Coin : AmountType()
    object Reserve : AmountType()
    data class Token(val token: BlockchainToken) : AmountType()
}
