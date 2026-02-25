package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import org.stellar.sdk.*
import org.stellar.sdk.operations.ChangeTrustOperation
import org.stellar.sdk.operations.CreateAccountOperation
import org.stellar.sdk.operations.Operation
import org.stellar.sdk.operations.PaymentOperation
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
        val uncompiledTransaction = transactionData.requireUncompiled()

        val amount = uncompiledTransaction.amount
        val fee = requireNotNull(uncompiledTransaction.fee?.amount?.longValue)
        val destinationKeyPair = KeyPair.fromAccountId(uncompiledTransaction.destinationAddress)
        val sourceKeyPair = KeyPair.fromAccountId(uncompiledTransaction.sourceAddress)
        val timeBounds = getTimeBounds(uncompiledTransaction)

        val stellarMemo = (uncompiledTransaction.extras as? StellarTransactionExtras)?.memo
        val memo = try {
            stellarMemo?.toStellarSdkMemo()
        } catch (exception: Exception) {
            return Result.Failure(exception.toBlockchainSdkError())
        }

        val amountType = uncompiledTransaction.amount.type
        val token = if (amountType is AmountType.Token) amountType.token else null
        val targetAccountResponse = when (
            val result =
                networkProvider.checkTargetAccount(uncompiledTransaction.destinationAddress, token)
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }

        return when (amount.type) {
            is AmountType.Coin -> {
                val operation =
                    if (targetAccountResponse.accountCreated) {
                        PaymentOperation.builder()
                            .destination(destinationKeyPair.accountId)
                            .asset(AssetTypeNative())
                            .amount(amount.value!!)
                            .build()
                    } else {
                        val amountValue = requireNotNull(amount.value)
                        if (amountValue < minReserve) {
                            val minAmount = Amount(minReserve, blockchain)
                            return Result.Failure(
                                BlockchainSdkError.CreateAccountUnderfunded(
                                    blockchain = blockchain,
                                    minReserve = minAmount,
                                ),
                            )
                        }
                        CreateAccountOperation.builder()
                            .destination(destinationKeyPair.accountId)
                            .startingBalance(amountValue)
                            .build()
                    }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee, timeBounds, memo)
                    ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())
            }
            is AmountType.Token -> {
                if (!targetAccountResponse.accountCreated) {
                    return Result.Failure(
                        BlockchainSdkError.CustomError(
                            "The destination account is not created. " +
                                "To create account send " +
                                "${minReserve.stripTrailingZeros().toPlainString()}+ " + "${blockchain.currency}.",
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

                val amountValue = amount.value
                val operation: Operation = if (amountValue != null) {
                    PaymentOperation.builder()
                        .destination(destinationKeyPair.accountId)
                        .asset(Asset.create(canonicalForm(amount.type.token.contractAddress)))
                        .amount(amountValue)
                        .build()
                } else {
                    ChangeTrustOperation.builder()
                        .asset(ChangeTrustAsset(Asset.create(canonicalForm(amount.type.token.contractAddress))))
                        .limit(CHANGE_TRUST_OPERATION_LIMIT.toBigDecimal())
                        .sourceAccount(sourceKeyPair.accountId)
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
        fee: Long,
        timeBounds: TimeBounds,
        memo: Memo?,
    ): Transaction? {
        val transactionBuilder = TransactionBuilder(
            Account(sourceKeyPair.accountId, sequence),
            network,
        )
        transactionBuilder
            .addOperation(this)
            .addPreconditions(
                TransactionPreconditions.builder()
                    .timeBounds(timeBounds)
                    .build(),
            )
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
        val requiredReserve = baseReserve.plus(requireNotNull(fee.value))
        val coinAmountValue = coinAmount.value
        if (coinAmountValue == null || requiredReserve > coinAmountValue) {
            return Result.Failure(BlockchainSdkError.Stellar.MinReserveRequired(requiredReserve, blockchain.currency))
        }
        val contractAddress: String = requireNotNull(transactionData.contractAddress)
        val asset = ChangeTrustAsset(Asset.create(canonicalForm(contractAddress)))

        val sourceKeyPair = KeyPair.fromAccountId(transactionData.sourceAddress)
        val timeBounds = getTimeBounds(transactionData)
        val operation = ChangeTrustOperation.builder()
            .asset(asset)
            .limit(CHANGE_TRUST_OPERATION_LIMIT.toBigDecimal())
            .sourceAccount(sourceKeyPair.accountId)
            .build()

        transaction = operation.toTransaction(
            sourceKeyPair = sourceKeyPair,
            sequence = sequence,
            fee = fee.longValue,
            timeBounds = timeBounds,
            memo = null,
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to assemble transaction"))

        return Result.Success(transaction.hash())
    }

    private fun canonicalForm(contractAddress: String) = contractAddress.replace(
        oldValue = TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR,
        newValue = STELLAR_SDK_CONTRACT_ADDRESS_SEPARATOR,
    )

    companion object {
        const val CHANGE_TRUST_OPERATION_LIMIT = "922337203685.4775807"
        const val TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR = "-"
        const val STELLAR_SDK_CONTRACT_ADDRESS_SEPARATOR = ":"
        const val CONTRACT_ADDRESS_IGNORING_SUFFIX = "-1"
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