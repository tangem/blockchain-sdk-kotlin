package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import org.stellar.sdk.*
import org.stellar.sdk.xdr.AccountID
import org.stellar.sdk.xdr.DecoratedSignature
import org.stellar.sdk.xdr.Signature
import org.stellar.sdk.xdr.SignatureHint
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Calendar

class StellarTransactionBuilder(
    private val networkProvider: StellarNetworkProvider,
    private val publicKey: ByteArray,
) {

    val network: Network = Network.PUBLIC
    private lateinit var transaction: Transaction
    var minReserve = 1.toBigDecimal()
    private val blockchain = Blockchain.Stellar

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    suspend fun buildToSign(transactionData: TransactionData, sequence: Long): Result<ByteArray> {
        transactionData.requireUncompiled()

        val amount = transactionData.amount
        val fee = requireNotNull(transactionData.fee?.amount?.longValue).toInt()
        val destinationKeyPair = KeyPair.fromAccountId(transactionData.destinationAddress)
        val sourceKeyPair = KeyPair.fromAccountId(transactionData.sourceAddress)
        val timeBounds = getTimeBounds(transactionData)

        val stellarMemo = (transactionData.extras as? StellarTransactionExtras)?.memo
        val memo = try {
            stellarMemo?.toStellarSdkMemo()
        } catch (exception: Exception) {
            return Result.Failure(exception.toBlockchainSdkError())
        }

        val amountType = transactionData.amount.type
        val token = if (amountType is AmountType.Token) amountType.token else null
        val targetAccountResponse = when (
            val result =
                networkProvider.checkTargetAccount(transactionData.destinationAddress, token)
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }

        return when (amount.type) {
            is AmountType.Coin -> {
                val operation =
                    if (targetAccountResponse.accountCreated) {
                        PaymentOperation.Builder(
                            destinationKeyPair.accountId,
                            AssetTypeNative(),
                            amount.value.toString(),
                        ).build()
                    } else {
                        if (amount.value!! < minReserve) {
                            val minAmount = Amount(minReserve, blockchain)
                            return Result.Failure(
                                BlockchainSdkError.CreateAccountUnderfunded(
                                    blockchain = blockchain,
                                    minReserve = minAmount,
                                ),
                            )
                        }
                        CreateAccountOperation.Builder(
                            destinationKeyPair.accountId,
                            amount.value.toString(),
                        ).build()
                    }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee, timeBounds, memo)
                    ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())
            }
            is AmountType.Token -> {
                if (!targetAccountResponse.accountCreated) {
                    return Result.Failure(
                        BlockchainSdkError.CustomError(
                            "The destination account is not created. To create account send 1+ XLM.",
                        ),
                    )
                }

                if (!targetAccountResponse.trustlineCreated!!) {
                    return Result.Failure(
                        BlockchainSdkError.CustomError(
                            "The destination account does not have a trustline for the asset being sent.",
                        ),
                    )
                }

                val operation: Operation = if (amount.value != null) {
                    PaymentOperation.Builder(
                        destinationKeyPair.accountId,
                        Asset.create(canonicalForm(amount.type.token.contractAddress)),
                        amount.value.toPlainString(),
                    )
                        .build()
                } else {
                    ChangeTrustOperation.Builder(
                        ChangeTrustAsset
                            .create(canonicalForm(amount.type.token.contractAddress)),
                        CHANGE_TRUST_OPERATION_LIMIT,
                    )
                        .setSourceAccount(sourceKeyPair.accountId)
                        .build()
                }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee, timeBounds, memo)
                    ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())
            }
            else -> Result.Failure(BlockchainSdkError.CustomError("Unknown amount Type"))
        }
    }

    @Suppress("MagicNumber")
    private fun getTimeBounds(transactionData: TransactionData.Uncompiled): TimeBounds {
        val calendar = transactionData.date ?: Calendar.getInstance()
        val minTime = 0L
        val maxTime = calendar.timeInMillis / 1000 + 120
        return TimeBounds(minTime, maxTime)
    }

    private fun Operation.toTransaction(
        sourceKeyPair: KeyPair,
        sequence: Long,
        fee: Int,
        timeBounds: TimeBounds,
        memo: Memo?,
    ): Transaction? {
        val accountID = AccountID()
        accountID.accountID = sourceKeyPair.xdrPublicKey

        val transactionBuilder = TransactionBuilder(
            Account(sourceKeyPair.accountId, sequence),
            network,
        )
        transactionBuilder
            .addOperation(this)
            .addTimeBounds(timeBounds)
            .setBaseFee(fee)

        if (memo != null) transactionBuilder.addMemo(memo)

        return transactionBuilder.build()
    }

    @Suppress("MagicNumber")
    fun buildToSend(signature: ByteArray): String {
        val hint = publicKey.takeLast(4).toByteArray()
        val decoratedSignature = DecoratedSignature().apply {
            this.hint = SignatureHint().apply { signatureHint = hint }
            this.signature = Signature().apply { this.signature = signature }
        }
        transaction.addSignature(decoratedSignature)
        return transaction.toEnvelopeXdrBase64()
    }

    fun getTransactionHash(): ByteArray = transaction.hash()

    fun buildToOpenTrustlineSign(
        transactionData: TransactionData.Uncompiled,
        baseReserve: BigDecimal,
        coinAmount: Amount,
        sequence: Long,
    ): Result<ByteArray> {
        val fee = requireNotNull(transactionData.fee?.amount)
        val requiredReserve = minReserve.plus(baseReserve).plus(requireNotNull(fee.value))
        if (requiredReserve > coinAmount.value) {
            return Result.Failure(BlockchainSdkError.Stellar.MinReserveRequired(requiredReserve))
        }
        val contractAddress: String = requireNotNull(transactionData.contractAddress)
        val asset = ChangeTrustAsset.create(canonicalForm(contractAddress))

        val sourceKeyPair = KeyPair.fromAccountId(transactionData.sourceAddress)
        val timeBounds = getTimeBounds(transactionData)
        val operation = ChangeTrustOperation
            .Builder(asset, CHANGE_TRUST_OPERATION_LIMIT)
            .setSourceAccount(sourceKeyPair.accountId)
            .build()

        transaction = operation.toTransaction(sourceKeyPair, sequence, fee.longValue.toInt(), timeBounds, null)
            ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to assemble transaction"))

        return Result.Success(transaction.hash())
    }

    private fun canonicalForm(contractAddress: String) = contractAddress.replace("-", ":")

    private companion object {
        const val CHANGE_TRUST_OPERATION_LIMIT = "900000000000.0000000"
    }
}

data class StellarTransactionExtras(val memo: StellarMemo) : TransactionExtras

sealed class StellarMemo {
    data class Text(val value: String) : StellarMemo()
    data class Id(val value: BigInteger) : StellarMemo()
    data class Hash(val value: ByteArray) : StellarMemo() // TODO: check, add return hash? Infos in iOS app?

    fun hasNonEmptyMemo(): Boolean {
        return when (this) {
            is Text -> value.isNotEmpty()
            is Id -> value != BigInteger.ZERO
            is Hash -> value.isNotEmpty()
        }
    }
}

private fun StellarMemo.toStellarSdkMemo(): Memo {
    return when (this) {
        is StellarMemo.Text -> Memo.text(this.value)
        is StellarMemo.Id -> {
            val id = this.value
            if (id !in BigInteger.ZERO..Long.MAX_VALUE.toBigInteger() * 2.toBigInteger()) { // ID is uint64
                error("ID value out of uint64 range")
            }
            Memo.id(id.toLong())
        }
        is StellarMemo.Hash -> Memo.hash(this.value)
    }
}