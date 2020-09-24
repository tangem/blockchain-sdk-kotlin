package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import org.stellar.sdk.*
import org.stellar.sdk.xdr.AccountID
import org.stellar.sdk.xdr.DecoratedSignature
import org.stellar.sdk.xdr.Signature
import org.stellar.sdk.xdr.SignatureHint
import java.util.*

class StellarTransactionBuilder(
        private val networkManager: StellarNetworkManager,
        private val publicKey: ByteArray
) {

    private lateinit var transaction: Transaction
    var minReserve = 1.toBigDecimal()
    private val blockchain = Blockchain.Stellar

    suspend fun buildToSign(transactionData: TransactionData, sequence: Long, fee: Int): Result<ByteArray> {

        val destinationKeyPair = KeyPair.fromAccountId(transactionData.destinationAddress)
        val sourceKeyPair = KeyPair.fromAccountId(transactionData.sourceAddress)

        return when (transactionData.amount.type) {
            AmountType.Coin -> {
                val operation =
                        if (networkManager.checkIsAccountCreated(transactionData.sourceAddress)) {
                            PaymentOperation.Builder(
                                    destinationKeyPair.accountId,
                                    AssetTypeNative(),
                                    transactionData.amount.value.toString()
                            ).build()
                        } else {
                            if (transactionData.amount.value!! < minReserve) {
                                return Result.Failure(
                                        CreateAccountUnderfunded(Amount(minReserve, blockchain))
                                )
                            }
                            CreateAccountOperation.Builder(
                                    destinationKeyPair.accountId,
                                    transactionData.amount.value.toString()
                            ).build()
                        }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee)
                        ?: return Result.Failure(Exception("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())

            }
            AmountType.Token -> {
                val keyPair = KeyPair.fromAccountId(transactionData.amount.address)
                val asset = Asset.createNonNativeAsset(
                        transactionData.amount.currencySymbol,
                        keyPair.accountId
                )
                val operation: Operation = if (transactionData.amount.value != null) {
                    PaymentOperation.Builder(
                            destinationKeyPair.accountId,
                            asset,
                            transactionData.amount.value!!.toPlainString())
                            .build()
                } else {
                    ChangeTrustOperation.Builder(asset, "900000000000.0000000")
                            .setSourceAccount(sourceKeyPair.accountId)
                            .build()
                }
                transaction = operation.toTransaction(sourceKeyPair, sequence, fee)
                        ?: return Result.Failure(Exception("Failed to assemble transaction")) // should not happen

                Result.Success(transaction.hash())
            }
            else -> Result.Failure(Exception("Unknown amount Type"))
        }
    }

        private fun Operation.toTransaction(
                sourceKeyPair: KeyPair,
                sequence: Long,
                fee: Int
        ): Transaction? {

            val accountID = AccountID()
            accountID.accountID = sourceKeyPair.xdrPublicKey
            val currentTime = Calendar.getInstance().timeInMillis / 1000
            val minTime = 0L
            val maxTime = currentTime + 120

            return Transaction.Builder(
                    Account(sourceKeyPair.accountId, sequence), networkManager.network)
                    .addOperation(this)
                    .addTimeBounds(TimeBounds(minTime, maxTime))
                    .setOperationFee(fee)
                    .build()
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