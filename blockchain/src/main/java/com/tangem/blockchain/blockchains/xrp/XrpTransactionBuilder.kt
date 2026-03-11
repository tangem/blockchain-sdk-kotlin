package com.tangem.blockchain.blockchains.xrp

import com.ripple.core.coretypes.AccountID
import com.ripple.core.coretypes.Currency
import com.ripple.core.coretypes.uint.UInt32
import com.ripple.core.fields.Field
import com.ripple.crypto.ecdsa.ECDSASignature
import com.ripple.utils.HashUtils
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.override.XrpPayment
import com.tangem.blockchain.blockchains.xrp.override.XrpSignedTransaction
import com.tangem.blockchain.blockchains.xrp.override.XrpTrustSet
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bigIntegerValue
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
    val blockchain = Blockchain.XRP

    private val canonicalPublicKey = XrpAddressService.canonizePublicKey(publicKey)
    private var transaction: XrpSignedTransaction? = null

    suspend fun buildToSign(transactionData: TransactionData): Result<ByteArray> {
        val uncompiledTransaction = transactionData.requireUncompiled()
        val sourceAddress = XrpAddressService.decodeXAddress(uncompiledTransaction.sourceAddress)
            ?.address ?: uncompiledTransaction.sourceAddress
        val sequence = networkProvider.getSequence(sourceAddress).successOr { return it }

        val preparedTx = buildPaymentTransaction(uncompiledTransaction, sequence).successOr { return it }
        transaction = preparedTx

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            Result.Success(preparedTx.signingData)
        } else {
            Result.Success(HashUtils.halfSha512(preparedTx.signingData))
        }
    }

    suspend fun buildToSignWithNoRipple(
        transactionData: TransactionData,
        sequenceOverride: Long,
    ): Result<Pair<ByteArray, XrpSignedTransaction>> {
        val uncompiledTransaction = transactionData.requireUncompiled()
        val preparedTx = buildPaymentTransaction(uncompiledTransaction, sequenceOverride).successOr { return it }

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            Result.Success(preparedTx.signingData to preparedTx)
        } else {
            Result.Success(HashUtils.halfSha512(preparedTx.signingData) to preparedTx)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun buildPaymentTransaction(
        transactionData: TransactionData.Uncompiled,
        sequence: Long,
    ): Result<XrpSignedTransaction> {
        val decodedXAddress = XrpAddressService.decodeXAddress(transactionData.destinationAddress)
        val destinationAddress = decodedXAddress?.address ?: transactionData.destinationAddress
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
        val checkResult = networkProvider.checkTargetAccount(destinationAddress, token)
            .successOr { return it }
        val isAccountCreated = checkResult.accountCreated
        val trustlineCreated = checkResult.trustlineCreated == true
        when (transactionData.amount.type) {
            AmountType.Coin -> if (!isAccountCreated && transactionData.amount.value!! < minReserve) {
                return Result.Failure(accountUnderfundedError())
            }
            is AmountType.Token -> {
                if (!isAccountCreated) return Result.Failure(accountUnderfundedTokenError())
                if (!trustlineCreated) return Result.Failure(notHaveTrustlineError())
            }
            is AmountType.TokenYieldSupply,
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> return Result.Failure(BlockchainSdkError.CustomError("Unknown amount Type"))
        }

        val payment = XrpPayment()
        payment.putTranslated(AccountID.Account, transactionData.sourceAddress)
        payment.putTranslated(AccountID.Destination, destinationAddress)
        if (transactionData.amount.type is AmountType.Token) {
            val (currency, issuer) = transactionData.amount.type
                .token.contractAddress.splitContractAddress()
            val amount = XrpAmount(transactionData.amount.value!!, currency, issuer)
            payment.amount(amount)

            val hasTransferRate = networkProvider.hasTransferRate(issuer.address)
            if (hasTransferRate) {
                payment.putTranslated(Field.Flags, UInt32(TF_PARTIAL_PAYMENT))
            }
        } else {
            payment.putTranslated(XrpAmount.Amount, transactionData.amount.bigIntegerValue().toString())
        }
        payment.putTranslated(UInt32.Sequence, sequence)
        payment.putTranslated(XrpAmount.Fee, transactionData.fee!!.amount.bigIntegerValue().toString())
        if (destinationTag != null) {
            payment.putTranslated(UInt32.DestinationTag, destinationTag)
        }

        return Result.Success(payment.prepare(canonicalPublicKey))
    }

    fun buildToSend(signature: ByteArray): String {
        val tx = transaction ?: error("No transaction to send")
        if (canonicalPublicKey[0] == 0xED.toByte()) {
            tx.addSign(signature)
        } else {
            val derSignature = encodeDerSignature(signature)
            tx.addSign(derSignature)
        }
        return tx.tx_blob
    }

    fun buildSecondaryToSend(signature: ByteArray, secondaryTransaction: XrpSignedTransaction): String {
        if (canonicalPublicKey[0] == 0xED.toByte()) {
            secondaryTransaction.addSign(signature)
        } else {
            val derSignature = encodeDerSignature(signature)
            secondaryTransaction.addSign(derSignature)
        }
        return secondaryTransaction.tx_blob
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
        val requiredReserve = reserveInc
            .plus(requireNotNull(fee.value))
        if (coinAmountValue == null || requiredReserve > coinAmountValue) {
            return Result.Failure(BlockchainSdkError.Xrp.MinReserveRequired(requiredReserve, blockchain.currency))
        }
        val contractAddress: String = requireNotNull(transactionData.contractAddress)

        val (currency, issuer) = contractAddress.splitContractAddress()
        val limitAmount = XrpAmount(BigDecimal("9999999999999999E80"), currency, issuer)

        val trustSet = XrpTrustSet()
        trustSet.putTranslated(AccountID.Account, transactionData.sourceAddress)
        trustSet.putTranslated(XrpAmount.Fee, fee.bigIntegerValue().toString())
        trustSet.putTranslated(UInt32.Sequence, sequence)
        trustSet.putTranslated(Field.Flags, UInt32(TF_SET_NO_RIPPLE))
        trustSet.limitAmount(limitAmount)

        transaction = trustSet.prepare(canonicalPublicKey)

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            Result.Success(transaction!!.signingData)
        } else {
            Result.Success(HashUtils.halfSha512(transaction!!.signingData))
        }
    }

    private fun accountUnderfundedError() = BlockchainSdkError.CreateAccountUnderfunded(
        blockchain = blockchain,
        minReserve = Amount(minReserve, blockchain),
    )

    private fun accountUnderfundedTokenError() = BlockchainSdkError.CustomError(
        "The destination account is not created. " +
            "To create account send ${minReserve.stripTrailingZeros().toPlainString()}+ ${blockchain.currency}.",
    )

    private fun notHaveTrustlineError() = BlockchainSdkError
        .CustomError("The destination account does not have a trustline for the asset being sent.")

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

    companion object {
        const val TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR = "."
        private const val TF_PARTIAL_PAYMENT = 131072 // 0x00020000 - for Payment tx
        private const val TF_SET_NO_RIPPLE = 131072 // 0x00020000 - for TrustSet tx
    }
}