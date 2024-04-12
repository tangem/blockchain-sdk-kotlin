package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleResult
import java.math.RoundingMode
import java.util.EnumSet

internal class KoinosWalletManager(
    wallet: Wallet,
    transactionHistoryProvider: TransactionHistoryProvider,
    private val networkService: KoinosNetworkService,
    private val transactionBuilder: KoinosTransactionBuilder,
) : WalletManager(wallet = wallet, transactionHistoryProvider = transactionHistoryProvider) {

    override val currentHost: String
        get() = networkService.baseUrl

    override suspend fun updateInternal() {
        val accountInfo = networkService.getInfo(wallet.address)
            .successOr { return }

        val unconfirmedOutgoingTransaction = wallet.recentTransactions.firstOrNull {
            it.status == TransactionStatus.Unconfirmed
        }

        if (unconfirmedOutgoingTransaction != null && wallet.getCoinAmount().value != accountInfo.koinBalance) {
            val recentTransactions = networkService.getTransactionHistory(
                address = wallet.address,
                pageSize = 20,
                sequenceNum = 0,
            ).successOr { null }

            if (recentTransactions != null && recentTransactions.any { it.id == unconfirmedOutgoingTransaction.hash }) {
                unconfirmedOutgoingTransaction.status = TransactionStatus.Confirmed
            }
        }

        wallet.changeAmountValue(AmountType.Coin, newValue = accountInfo.koinBalance)

        // TODO maybe change interface ([REDACTED_TASK_KEY])
        wallet.additionalInfo = WalletAdditionalInfo.Koinos(
            mana = accountInfo.mana,
            timeToChargeMillis = 0L,
        )
        // TODO [REDACTED_TASK_KEY]
        // wallet.changeAmountValue(AmountType.KoinosMana, newValue = accountInfo.mana)
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        // TODO [REDACTED_TASK_KEY] validate with mana amount
        return super.validateTransaction(amount, fee)
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val nonce = networkService.getCurrentNonce(wallet.address)
            .successOr { return SimpleResult.fromTangemSdkError(it.error) }

        // FIXME remove after UI requirements is ready ([REDACTED_TASK_KEY])
        val testManaLimit = (wallet.additionalInfo as WalletAdditionalInfo.Koinos)
            .mana.multiply(0.1.toBigDecimal())
            .setScale(Blockchain.Koinos.decimals(), RoundingMode.UP)

        // val testManaLimit = wallet.amounts[AmountType.KoinosMana]!!.value!!
        //     .multiply(0.1.toBigDecimal())
        //     .setScale(Blockchain.Koinos.decimals(), RoundingMode.UP)

        // FIXME use this instead ([REDACTED_TASK_KEY])
        // val manaLimit = (transactionData.extras as? KoinosTransactionExtras)?.manaLimit
        //     ?: return SimpleResult.fromTangemSdkError(BlockchainSdkError.FailedToBuildTx)

        val transactionDataWithMana = transactionData.copy(
            extras = KoinosTransactionExtras(testManaLimit),
        )

        val (transaction, hashToSign) = transactionBuilder.buildToSign(
            transactionData = transactionDataWithMana,
            currentNonce = nonce,
        ).successOr { return SimpleResult.fromTangemSdkError(it.error) }

        val signature = signer.sign(hashToSign, wallet.publicKey)
            .successOr { return SimpleResult.fromTangemSdkError(it.error) }

        val signedTransaction = transactionBuilder.buildToSend(transaction, signature)

        val transactionRes = networkService.submitTransaction(signedTransaction)
            .successOr { return it.toSimpleResult() }

        wallet.addOutgoingTransaction(
            transactionData.copy(
                hash = transactionRes.id,
            ),
        )

        return SimpleResult.Success
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        // TODO ([REDACTED_TASK_KEY])
        // val currentMana = wallet.amounts[AmountType.KoinosMana]!!.value!!
        //     .multiply(0.1.toBigDecimal())
        //     .setScale(Blockchain.Koinos.decimals(), RoundingMode.UP)
        //
        // return Result.Success(
        //     TransactionFee.Single(
        //         normal = Fee.Common(
        //             amount = Amount(currentMana, Blockchain.Koinos, AmountType.KoinosMana)
        //         )
        //     )
        // )

        return Result.Success(TransactionFee.Single(normal = Fee.Common(amount = Amount(Blockchain.Koinos))))
    }
}