package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.walletcore.CardanoTWTxBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.trustWalletCoinType
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Cardano
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.math.BigDecimal
import kotlin.properties.Delegates

// You can decode your CBOR transaction here: https://cbor.me
internal class CardanoTransactionBuilder(
    private val wallet: Wallet,
) : TransactionValidator {

    private val coinType: CoinType = wallet.blockchain.trustWalletCoinType
    private val decimals: Int = wallet.blockchain.decimals()

    private var twTxBuilder: CardanoTWTxBuilder by Delegates.notNull()

    fun update(outputs: List<CardanoUnspentOutput>) {
        twTxBuilder = CardanoTWTxBuilder(wallet, outputs)
    }

    override fun validate(transaction: TransactionData): Result<Unit> {
        return runCatching {
            val isCoinTransaction = transaction.amount.type is AmountType.Coin
            val transactionValue = transaction.amount.value ?: BigDecimal.ZERO

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientSendingAdaAmount,
                condition = isCoinTransaction && transactionValue < BigDecimal.ONE,
            )

            val plan = AnySigner.plan(
                twTxBuilder.build(transaction),
                coinType,
                Cardano.TransactionPlan.parser(),
            )

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientMinAdaBalanceToSendToken,
                condition = !isCoinTransaction && plan.error == Common.SigningError.Error_low_balance,
            )

            checkRemainingAdaBalance(amount = transaction.amount, change = plan.change)
        }
    }

    fun estimateFee(transaction: TransactionData): Fee {
        val input = twTxBuilder.build(transaction)
        val plan = AnySigner.plan(input, coinType, Cardano.TransactionPlan.parser())

        return when (val type = transaction.amount.type) {
            AmountType.Coin -> {
                Fee.Common(
                    amount = transaction.amount.copy(
                        value = BigDecimal(plan.fee).movePointLeft(decimals),
                    ),
                )
            }
            is AmountType.Token -> {
                Fee.CardanoToken(
                    amount = transaction.amount.copy(
                        value = BigDecimal(plan.fee).movePointLeft(type.token.decimals),
                    ),
                    minAdaValue = BigDecimal(plan.amount).movePointLeft(decimals),
                )
            }
            else -> throw BlockchainSdkError.CustomError("AmountType $type is not supported")
        }
    }

    fun buildForSign(transaction: TransactionData): ByteArray {
        val input = twTxBuilder.build(transaction)
        val txInputData = input.toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(transaction: TransactionData, signatureInfo: SignatureInfo): ByteArray {
        val input = twTxBuilder.build(transaction)
        val txInputData = input.toByteArray()

        val signatures = DataVector()
        signatures.add(signatureInfo.signature)

        val publicKeys = DataVector()

        // WalletCore used here `.ed25519Cardano` curve with 128 bytes publicKey.
        // Calculated as: chainCode + secondPubKey + chainCode
        // The number of bytes in a Cardano public key (two ed25519 public key + chain code).
        // We should add dummy chain code in publicKey if we use old 32 byte key to get 128 bytes in total
        val publicKey = if (CardanoUtils.isExtendedPublicKey(signatureInfo.publicKey)) {
            signatureInfo.publicKey
        } else {
            signatureInfo.publicKey + ByteArray(MISSING_LENGTH_TO_EXTENDED_KEY)
        }

        publicKeys.add(publicKey)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            txInputData,
            signatures,
            publicKeys,
        )

        val output = Cardano.SigningOutput.parseFrom(compileWithSignatures)

        if (output.error != Common.SigningError.OK || output.encoded.isEmpty) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return output.encoded.toByteArray()
    }

    private fun checkRemainingAdaBalance(amount: Amount, change: Long) {
        val notZeroTokens = calculateRemainingNotZeroTokensBalances(transactionAmount = amount)

        if (notZeroTokens.isEmpty()) {
            val minChange = BigDecimal.ONE.movePointRight(decimals).toLong()

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientRemainingBalance,
                condition = change in 1 until minChange,
            )
        } else {
            val minChange = twTxBuilder.calculateMinAdaValueToWithdrawAllTokens(tokens = wallet.getTokens())

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens,
                condition = change == 0L || change in 1 until minChange,
            )
        }
    }

    private fun calculateRemainingNotZeroTokensBalances(transactionAmount: Amount): List<Amount> {
        val transactionToken = (transactionAmount.type as? AmountType.Token)?.token

        return wallet.amounts
            .mapNotNull { amount ->
                val type = amount.value.type

                if (type is AmountType.Token) {
                    // calculate remaining token balance
                    if (type.token == transactionToken) {
                        val transactionValue = transactionAmount.value ?: BigDecimal.ZERO
                        amount.value - transactionValue
                    } else {
                        amount.value
                    }
                } else {
                    null
                }
            }
            .filter { it.longValueOrZero != 0L }
    }

    private fun throwIf(exception: BlockchainSdkError.Cardano, condition: Boolean) {
        if (condition) throw exception
    }

    private companion object {
        const val MISSING_LENGTH_TO_EXTENDED_KEY = 32 * 3
    }
}