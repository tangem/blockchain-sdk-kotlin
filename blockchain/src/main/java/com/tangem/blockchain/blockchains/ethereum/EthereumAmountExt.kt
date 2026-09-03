package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Number of decimals this amount is denominated in on the chain.
 *
 * Coin amounts follow [Blockchain.onChainDecimals], which differs from [Amount.decimals] on Arc, where the coin
 * has 6 decimals while the chain operates with 18. Token amounts always follow their contract decimals.
 */
internal fun Amount.onChainDecimals(blockchain: Blockchain): Int {
    return if (type == AmountType.Coin) blockchain.onChainDecimals() else decimals
}

/** Value of this amount in the smallest unit the chain operates with, or `null` if the value is unknown */
internal fun Amount.toOnChainValue(blockchain: Blockchain): BigInteger? {
    return toOnChainDecimalValue(blockchain)?.toBigInteger()
}

internal fun Amount.toOnChainDecimalValue(blockchain: Blockchain): BigDecimal? {
    return value?.movePointRight(onChainDecimals(blockchain))
}