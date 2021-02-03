package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import org.stellar.sdk.*
import org.stellar.sdk.xdr.AccountID
import org.stellar.sdk.xdr.DecoratedSignature
import org.stellar.sdk.xdr.Signature
import org.stellar.sdk.xdr.SignatureHint
import java.math.BigInteger
import java.util.*

class StellarTransactionBuilder(
        private val networkService: StellarNetworkService,
        private val publicKey: ByteArray,
        private val calendar: Calendar = Calendar.getInstance(),
) {

    val network: Network = Network.PUBLIC
    private lateinit var transaction: Transaction
    var minReserve = 1.toBigDecimal()
    private val blockchain = Blockchain.Stellar

    suspend fun buildToSign(transactionData: TransactionData, sequence: Long, fee: Int): Result<ByteArray> { //TODO: use fee from transaction data

        val amount = transactionData.amount
        val destinationKeyPair = KeyPair.fromAccountId(transactionData.destinationAddress)
        val sourceKeyPair = KeyPair.fromAccountId(transactionData.sourceAddress)

        val stellarMemo = (transactionData.extras as? StellarTransactionExtras)?.memo
        val memo = try {
            stellarMemo?.toStellarSdkMemo()
        } catch (exception: Exception) {
            return Result.Failure(exception)
        }

        return when (amount.type) {
            is AmountType.Coin -> {
                val operation =
                        if (networkService.checkIsAccountCreated(transactionData.destinationAddress)) {
                            PaymentOperation.Builder(
                                    destinationKeyPair.accountId,
                                    AssetTypeNative(),
                                    amount.value.toString()
                            ).build()
                        } else {
                            if (amount.value!! < minReserve) {
                                return Result.Failure(
                                        CreateAccountUnderfunded(Amount(minReserve, blockchain))
                                )
                            }
                            CreateAccountOperation.Builder(
                                    destinationKeyPair.accountId,
                                    amount.value.toString()
                            ).build()
                        }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee, memo)
                        ?: return Result.Failure(Exception("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())

            }
            is AmountType.Token -> {
                val asset = Asset.createNonNativeAsset(
                        amount.currencySymbol,
                        amount.type.token.contractAddress
                )
                val operation: Operation = if (amount.value != null) {
                    PaymentOperation.Builder(
                            destinationKeyPair.accountId,
                            asset,
                            amount.value!!.toPlainString())
                            .build()
                } else {
                    ChangeTrustOperation.Builder(asset, "900000000000.0000000")
                            .setSourceAccount(sourceKeyPair.accountId)
                            .build()
                }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee, memo)
                        ?: return Result.Failure(Exception("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())
            }
            else -> Result.Failure(Exception("Unknown amount Type"))
        }
    }

    private fun Operation.toTransaction(
            sourceKeyPair: KeyPair,
            sequence: Long,
            fee: Int,
            memo: Memo?
    ): Transaction? {

        val accountID = AccountID()
        accountID.accountID = sourceKeyPair.xdrPublicKey
        val currentTime = calendar.timeInMillis / 1000
        val minTime = 0L
        val maxTime = currentTime + 120

        val transactionBuilder = Transaction.Builder(
                Account(sourceKeyPair.accountId, sequence),
                network
        )
        transactionBuilder
                .addOperation(this)
                .addTimeBounds(TimeBounds(minTime, maxTime))
                .setOperationFee(fee)

        if (memo != null) transactionBuilder.addMemo(memo)

        return transactionBuilder.build()
    }

    fun buildToSend(signature: ByteArray): String {
        val hint = publicKey.takeLast(4).toByteArray()
        val decoratedSignature = DecoratedSignature().apply {
            this.hint = SignatureHint().apply { signatureHint = hint }
            this.signature = Signature().apply { this.signature = signature }
        }
        transaction.signatures.add(decoratedSignature)
        return transaction.toEnvelopeXdrBase64()
    }

    fun getTransactionHash() = transaction.hash()
}

data class StellarTransactionExtras(val memo: StellarMemo) : TransactionExtras

sealed class StellarMemo {
    data class Text(val value: String) : StellarMemo()
    data class Id(val value: BigInteger) : StellarMemo()
    data class Hash(val value: ByteArray) : StellarMemo() // TODO: check, add return hash? Infos in iOS app?
}

private fun StellarMemo.toStellarSdkMemo(): Memo {
    return when (this) {
        is StellarMemo.Text -> Memo.text(this.value)
        is StellarMemo.Id -> {
            val id = this.value
            if (id !in BigInteger.ZERO..(Long.MAX_VALUE.toBigInteger() * 2.toBigInteger())) { // ID is uint64
                throw Exception("ID value out of uint64 range")
            }
            Memo.id(id.toLong())
        }
        is StellarMemo.Hash -> Memo.hash(this.value)
    }
}