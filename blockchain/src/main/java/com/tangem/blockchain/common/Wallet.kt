package com.tangem.blockchain.common

import com.tangem.blockchain.common.address.Address
import com.tangem.common.extensions.calculateHashCode
import com.tangem.crypto.hdWallet.DerivationPath
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

class Wallet(
    val blockchain: Blockchain,
    val addresses: Set<Address>,
    val publicKey: PublicKey,
    tokens: Set<Token>,
) {
    //we put only unconfirmed transactions here, but never delete them, change status to confirmed instead
    val recentTransactions: MutableList<TransactionData> = mutableListOf()
    // transactions from history provider
    val historyTransactions: MutableList<TransactionData> = mutableListOf()
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

    fun removeAllTokens() {
        val coin = amounts[AmountType.Coin]
        amounts.clear()
        coin?.let { amounts[AmountType.Coin] = it }
    }

    fun setReserveValue(value: BigDecimal) = setAmount(Amount(value, blockchain, AmountType.Reserve))

    fun getTokenAmount(token: Token): Amount? {
        val key = amounts.keys.find { it is AmountType.Token && it.token == token }
        return amounts[key]
    }

    fun getTokens(): Set<Token> = amounts.keys.filterIsInstance<AmountType.Token>().map { it.token }.toSet()

    fun addTransactionDummy(direction: TransactionDirection? = null) {
        var sourceAddress = "unknown"
        var destinationAddress = "unknown"
        when (direction) {
            TransactionDirection.Outgoing -> sourceAddress = address
            TransactionDirection.Incoming -> destinationAddress = address
            else -> {}
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

    fun addOutgoingTransaction(transactionData: TransactionData, hashToLowercase: Boolean = true) {
        transactionData.apply {
            date = Calendar.getInstance()
            if (hashToLowercase) hash = hash?.toLowerCase(Locale.US)
        }
        if (recentTransactions.any { it.hash == transactionData.hash }) return

        recentTransactions.add(transactionData)
    }

    fun fundsAvailable(amountType: AmountType): BigDecimal {
        return amounts[amountType]?.value ?: BigDecimal.ZERO
    }

    fun getExploreUrl(address: String? = null, token: Token? = null) =
        blockchain.getExploreUrl(address ?: this.address, token?.contractAddress)

    fun getShareUri(address: String? = null) =
        blockchain.getShareUri(address ?: this.address)

    data class PublicKey(
        val seedKey: ByteArray,
        val derivedKey: ByteArray?,
        val derivationPath: DerivationPath?,
    ) {
        val blockchainKey: ByteArray = derivedKey ?: seedKey

        override fun equals(other: Any?): Boolean {
            val other = other as? PublicKey ?: return false

            if (!seedKey.contentEquals(other.seedKey)) return false
            if (!derivedKey.contentEquals(other.derivedKey)) return false

            return when {
                derivationPath == null && other.derivationPath == null -> true
                derivationPath == null -> false
                else -> derivationPath == other.derivationPath
            }
        }

        override fun hashCode(): Int {
            return calculateHashCode(
                seedKey.contentHashCode(),
                derivedKey?.contentHashCode() ?: 0,
                derivationPath?.hashCode() ?: 0
            )
        }
    }
}