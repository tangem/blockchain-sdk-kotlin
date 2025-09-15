package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

class Wallet(
    val blockchain: Blockchain,
    var addresses: Set<Address>,
    val publicKey: PublicKey,
    tokens: Set<Token>,
) {

    var ens: String? = null
        private set

    // we put only unconfirmed transactions here, but never delete them, change status to confirmed instead
    val recentTransactions: MutableList<TransactionData.Uncompiled> = mutableListOf()
    val amounts: MutableMap<AmountType, Amount> = mutableMapOf()
    val address: String
        get() = addresses.find { it.type == AddressType.Default }?.value
            ?: error("Addresses must contain default address")

    init {
        setAmount(Amount(null, blockchain, AmountType.Coin))
        tokens.forEach { setAmount(Amount(it)) }
    }

    internal fun setEnsName(name: String?) {
        ens = name
    }

    fun setAmount(amount: Amount) {
        amounts[amount.type] = amount
    }

    fun setAmount(value: BigDecimal?, amountType: AmountType, maxValue: BigDecimal? = null) {
        setAmount(Amount(value = value, blockchain = blockchain, type = amountType, maxValue = maxValue))
    }

    fun changeAmountValue(amountType: AmountType, newValue: BigDecimal?, newMaxValue: BigDecimal? = null) {
        amounts[amountType]?.let {
            amounts[amountType] = it.copy(value = newValue, maxValue = newMaxValue)
        }
    }

    fun setCoinValue(value: BigDecimal) = setAmount(Amount(value, blockchain, AmountType.Coin))

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

    fun getCoinAmount(): Amount {
        return requireNotNull(amounts[AmountType.Coin]) {
            "Coin Amount is NULL, but it can't be, because it setup on init"
        }
    }

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

        val transaction = TransactionData.Uncompiled(
            amount = Amount(null, blockchain),
            fee = null,
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            date = Calendar.getInstance(),
        )
        recentTransactions.add(transaction)
    }

    fun addOutgoingTransaction(transactionData: TransactionData, hashToLowercase: Boolean = true) {
        if (transactionData is TransactionData.Uncompiled) {
            transactionData.apply {
                date = Calendar.getInstance()
                if (hashToLowercase) hash = hash?.lowercase(Locale.US)
            }
            if (recentTransactions.any { it.hash == transactionData.hash }) return

            recentTransactions.add(transactionData)
        } else {
            // TODO staking [REDACTED_TASK_KEY]
        }
    }

    fun fundsAvailable(amountType: AmountType): BigDecimal {
        return amounts[amountType]?.value ?: BigDecimal.ZERO
    }

    fun getExploreUrl(address: String? = null, token: Token? = null) =
        blockchain.getExploreUrl(address ?: this.address, token?.contractAddress)

    fun getShareUri(address: String? = null) = blockchain.getShareUri(address ?: this.address)

    class HDKey(
        val extendedPublicKey: ExtendedPublicKey,
        val path: DerivationPath,
    )

    class PublicKey(
        val seedKey: ByteArray,
        val derivationType: DerivationType?,
    ) {

        val blockchainKey: ByteArray
            get() = when (derivationType) {
                null -> seedKey
                is DerivationType.Plain -> derivationType.hdKey.extendedPublicKey.publicKey
                is DerivationType.Double -> {
                    CardanoUtils.extendPublicKey(
                        derivationType.first.extendedPublicKey,
                        derivationType.second.extendedPublicKey,
                    )
                }
            }

        val derivationPath = derivationType?.hdKey?.path

        val derivedKey = derivationType?.hdKey?.extendedPublicKey?.publicKey

        sealed class DerivationType {

            abstract val hdKey: HDKey

            class Plain(override val hdKey: HDKey) : DerivationType()

            class Double(val first: HDKey, val second: HDKey) : DerivationType() {

                override val hdKey = first
            }
        }
    }
}