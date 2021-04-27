package com.tangem.blockchain.common

import com.tangem.blockchain.common.address.Address
import java.math.BigDecimal
import java.util.*

class Wallet(
        val cardId: String,
        val blockchain: Blockchain,
        val addresses: Set<Address>,
        val publicKey: ByteArray,
        tokens: Set<Token>
) {
    val recentTransactions: MutableList<TransactionData> = mutableListOf() //we put only unconfirmed transactions here, but never delete them, change status to confirmed instead
    val amounts: MutableMap<AmountType, Amount> = mutableMapOf()
    val address = addresses.find { it.type == blockchain.defaultAddressType() }?.value
            ?: throw Exception("Addresses must contain default address")

    init {
        setAmount(Amount(null, blockchain, AmountType.Coin))
        tokens.forEach { setAmount(Amount(it)) }
    }

    fun setAmount(amount: Amount) {
        amounts[amount.type] = amount
    }

    fun setCoinValue(value: BigDecimal) =
            setAmount(Amount(value, blockchain, AmountType.Coin))

    fun addTokenValue(value: BigDecimal, token: Token): Amount {
        val amount = Amount(token, value)
        setAmount(amount)
        return amount
    }

    fun removeToken(token: Token) {
        amounts.remove(AmountType.Token(token))
    }

    fun setReserveValue(value: BigDecimal) =
            setAmount(Amount(value, blockchain, AmountType.Reserve))

    fun getTokenAmount(token: Token): Amount? {
        val key = amounts.keys.find { it is AmountType.Token && it.token == token }
        return amounts[key]
    }

    fun getTokens(): Set<Token> =
            amounts.keys.filterIsInstance<AmountType.Token>().map { it.token }.toSet()

    fun addTransactionDummy(direction: TransactionDirection? = null) {
        var sourceAddress = "unknown"
        var destinationAddress = "unknown"
        when (direction) {
            TransactionDirection.Outgoing -> sourceAddress = address
            TransactionDirection.Incoming -> destinationAddress = address
        }

        val transaction = TransactionData(
                amount = Amount(null, blockchain),
                fee = null,
                sourceAddress = sourceAddress,
                destinationAddress = destinationAddress,
                date = Calendar.getInstance()
        )
        recentTransactions.add(transaction)
    }

    fun addOutgoingTransaction(transactionData: TransactionData) {
        transactionData.apply {
            date = Calendar.getInstance()
            hash = hash?.toLowerCase(Locale.US)
        }
        recentTransactions.add(transactionData)
    }

    fun fundsAvailable(amountType: AmountType): BigDecimal {
        return amounts[amountType]?.value ?: BigDecimal.ZERO
    }

    fun getExploreUrl(address: String? = null, token: Token? = null) =
            blockchain.getExploreUrl(address ?: this.address, token?.contractAddress)

    fun getShareUri(address: String? = null) =
            blockchain.getShareUri(address ?: this.address)
}