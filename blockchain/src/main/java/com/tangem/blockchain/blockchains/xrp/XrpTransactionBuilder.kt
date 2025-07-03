package com.tangem.blockchain.blockchains.xrp

import com.ripple.core.coretypes.AccountID
import com.ripple.core.coretypes.Currency
import com.ripple.core.coretypes.uint.UInt32
import com.ripple.crypto.ecdsa.ECDSASignature
import com.ripple.utils.HashUtils
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTokenBalance
import com.tangem.blockchain.blockchains.xrp.override.XrpPayment
import com.tangem.blockchain.blockchains.xrp.override.XrpSignedTransaction
import com.tangem.blockchain.blockchains.xrp.override.XrpTrustSet
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.orZero
import com.tangem.blockchain.extensions.successOr
import org.bitcoinj.core.ECKey
import java.math.BigDecimal
import java.math.BigInteger
import com.ripple.core.coretypes.Amount as XrpAmount

@Suppress("MagicNumber")
class XrpTransactionBuilder(private val networkProvider: XrpNetworkProvider, publicKey: ByteArray) {
    // https://xrpl.org/blog/2021/reserves-lowered.html
    var minReserve = 1.toBigDecimal()
    var reserveInc = 0.2.toBigDecimal()
    var tokenBalances = setOf<XrpTokenBalance>()
    val blockchain = Blockchain.XRP

    private val canonicalPublicKey = XrpAddressService.canonizePublicKey(publicKey)
    private var transaction: XrpSignedTransaction? = null

    suspend fun buildToSign(transactionData: TransactionData): Result<ByteArray> {
        transactionData.requireUncompiled()

        val decodedXAddress = XrpAddressService.decodeXAddress(transactionData.destinationAddress)
        val destinationAddress = decodedXAddress?.address ?: transactionData.destinationAddress
        val sourceAddress = XrpAddressService.decodeXAddress(transactionData.sourceAddress)
            ?.address ?: transactionData.sourceAddress
        val xAddressDestinationTag = decodedXAddress?.destinationTag

        val destinationTag = if (transactionData.extras is XrpTransactionExtras) {
            val extrasTag = transactionData.extras.destinationTag
            if (xAddressDestinationTag != null && xAddressDestinationTag != extrasTag) {
                return Result.Failure(BlockchainSdkError.CustomError("Two distinct destination tags found"))
            }
            extrasTag
        } else {
            xAddressDestinationTag
        }
        val token = (transactionData.amount.type as? AmountType.Token)?.token
        var isAccountCreated = false
        var trustlineCreated = false
        val sequence = networkProvider.getSequence(sourceAddress).successOr { return it }
        networkProvider.checkTargetAccount(destinationAddress, token)
            .map {
                isAccountCreated = it.accountCreated
                trustlineCreated = it.trustlineCreated ?: false
            }

        if (!isAccountCreated && transactionData.amount.value!! < minReserve) {
            return Result.Failure(
                BlockchainSdkError.CreateAccountUnderfunded(blockchain, Amount(minReserve, blockchain)),
            )
        }
        if (transactionData.amount.type is AmountType.Token && !trustlineCreated) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "The destination account does not have a trustline for the asset being sent.",
                ),
            )
        }

        val payment = XrpPayment()
        payment.putTranslated(AccountID.Account, transactionData.sourceAddress)
        payment.putTranslated(AccountID.Destination, destinationAddress)
        if (transactionData.amount.type is AmountType.Token) {
            val (currency, issuer) = transactionData.amount.type
                .token.contractAddress.splitContractAddress()
            val amount = XrpAmount(transactionData.amount.value!!, currency, issuer)
            payment.amount(amount)
        } else {
            payment.putTranslated(XrpAmount.Amount, transactionData.amount.bigIntegerValue().toString())
        }
        payment.putTranslated(UInt32.Sequence, sequence)
        payment.putTranslated(XrpAmount.Fee, transactionData.fee!!.amount.bigIntegerValue().toString())
        if (destinationTag != null) {
            payment.putTranslated(UInt32.DestinationTag, destinationTag)
        }

        transaction = payment.prepare(canonicalPublicKey)

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            Result.Success(transaction!!.signingData)
        } else {
            Result.Success(HashUtils.halfSha512(transaction!!.signingData))
        }
    }

    fun buildToSend(signature: ByteArray): String {
        if (canonicalPublicKey[0] == 0xED.toByte()) {
            transaction!!.addSign(signature)
        } else {
            val derSignature = encodeDerSignature(signature)
            transaction!!.addSign(derSignature)
        }
        return transaction!!.tx_blob
    }

    fun getTransactionHash() = transaction?.hash?.bytes()

    suspend fun buildToOpenTrustlineSign(
        transactionData: TransactionData.Uncompiled,
        coinAmount: Amount,
    ): Result<ByteArray> {
        val sourceAddress = XrpAddressService.decodeXAddress(transactionData.sourceAddress)
            ?.address ?: transactionData.sourceAddress
        val sequence = networkProvider.getSequence(sourceAddress).successOr { return it }
        val fee = requireNotNull(transactionData.fee?.amount)
        val coinAmountValue = coinAmount.value
        var requiredReserve = reserveInc
            .plus(requireNotNull(fee.value))
        if (coinAmountValue == null) requiredReserve = requiredReserve.plus(minReserve)
        if (requiredReserve > coinAmountValue.orZero()) {
            return Result.Failure(BlockchainSdkError.Xrp.MinReserveRequired(requiredReserve, blockchain.currency))
        }
        val contractAddress: String = requireNotNull(transactionData.contractAddress)

        val (currency, issuer) = contractAddress.splitContractAddress()
        val limitAmount = XrpAmount(BigDecimal("9999999999999999E80"), currency, issuer)

        val trustSet = XrpTrustSet()
        trustSet.putTranslated(AccountID.Account, transactionData.sourceAddress)
        trustSet.putTranslated(XrpAmount.Fee, fee.bigIntegerValue().toString())
        trustSet.putTranslated(UInt32.Sequence, sequence)
        trustSet.limitAmount(limitAmount)

        transaction = trustSet.prepare(canonicalPublicKey)

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            Result.Success(transaction!!.signingData)
        } else {
            Result.Success(HashUtils.halfSha512(transaction!!.signingData))
        }
    }

    private fun String.splitContractAddress(): Pair<Currency, AccountID> {
        val split = this.split(".")
        val currency = Currency.fromString(split.first())
        val issuer = AccountID.fromString(split[1])
        return currency to issuer
    }

    private fun encodeDerSignature(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val ecdsaSignature = ECDSASignature(r, canonicalS)
        return ecdsaSignature.encodeToDER()
    }

    data class XrpTransactionExtras(val destinationTag: Long) : TransactionExtras
}