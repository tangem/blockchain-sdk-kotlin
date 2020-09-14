package com.tangem.blockchain.common

import java.math.BigDecimal
import java.util.*

class Wallet(
        val blockchain: Blockchain,
        val address: String,
        val token: Token? = null
) {
    val exploreUrl: String
    val shareUrl: String
    val recentTransactions: MutableList<TransactionData> = mutableListOf() //we put only unconfirmed transactions here, but never delete them, change status to confirmed instead
    val amounts: MutableMap<AmountType, Amount> = mutableMapOf()
    var sentTransactionsCount: Int = 0

    init {
        setAmount(Amount(null, blockchain, address))
        if (token != null) setAmount(Amount(token))

        exploreUrl = blockchain.getExploreUrl(address, token)
        shareUrl = blockchain.getShareUri(address)
    }

    fun setAmount(amount: Amount) {
        amounts[amount.type] = amount
    }

    fun setCoinValue(value: BigDecimal) {
        val amount = Amount(value, blockchain, address)
        setAmount(amount)
    }

    fun setTokenValue(value: BigDecimal) {
        if (token != null) {
            val amount = Amount(token, value)
            setAmount(amount)
        }
    }

    fun setReserveValue(value: BigDecimal) {
        val amount = Amount(value, blockchain, address, AmountType.Reserve)
        setAmount(amount)
    }

    fun addIncomingTransactionDummy() { // TODO: do we still need this?
        val dummyAmount = Amount(null, blockchain)
        val transaction = TransactionData(dummyAmount, dummyAmount,
                "unknown", address, date = Calendar.getInstance()
        )
        recentTransactions.add(transaction)
    }

    fun fundsAvailable(amountType: AmountType): BigDecimal {
        return amounts[amountType]?.value ?: BigDecimal.ZERO
    }

}