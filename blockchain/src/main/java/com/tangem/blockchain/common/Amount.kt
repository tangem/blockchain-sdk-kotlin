package com.tangem.blockchain.common

import java.math.BigDecimal

data class Amount(
        val currencySymbol: String,
        var value: BigDecimal? = null,
        val decimals: Int,
        val type: AmountType = AmountType.Coin
) {
    constructor(
            value: BigDecimal?,
            blockchain: Blockchain,
            type: AmountType = AmountType.Coin
    ) : this(blockchain.currency, value, blockchain.decimals(), type)

    constructor(token: Token, value: BigDecimal? = null) :
            this(token.symbol, value, token.decimals, AmountType.Token(token))

    constructor(amount: Amount, value: BigDecimal) :
            this(amount.currencySymbol, value, amount.decimals, amount.type)

    constructor(blockchain: Blockchain) :
            this(blockchain.currency, BigDecimal.ZERO, blockchain.decimals(), AmountType.Coin)

    val longValue
        get() = value?.movePointRight(decimals)?.toLong()
}

sealed class AmountType {
    object Coin : AmountType()
    object Reserve : AmountType()
    data class Token(val token: com.tangem.blockchain.common.Token) : AmountType()
}