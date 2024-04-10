package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleResult

internal class KoinosWalletManager(
    wallet: Wallet,
    private val networkService: KoinosNetworkService,
    private val transactionBuilder: KoinosTransactionBuilder,
) : WalletManager(wallet = wallet) {

    override val currentHost: String
        get() = networkService.baseUrl

    override suspend fun updateInternal() {
        val accountInfo = networkService.getInfo(wallet.address)
            .successOr { return }

        wallet.changeAmountValue(AmountType.Coin, newValue = accountInfo.koinBalance)

        // TODO maybe change interface ([REDACTED_TASK_KEY])
        wallet.additionalInfo = WalletAdditionalInfo.Koinos(
            mana = accountInfo.mana,
            timeToChargeMillis = 0L,
        )

        // TODO add transaction history update ([REDACTED_TASK_KEY])
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val nonce = networkService.getCurrentNonce(wallet.address)
            .successOr { return SimpleResult.fromTangemSdkError(it.error) }

        // FIXME remove after UI requirements is ready ([REDACTED_TASK_KEY])
        val testManaLimit = (wallet.additionalInfo as WalletAdditionalInfo.Koinos)
            .mana.multiply(0.1.toBigDecimal())
            .setScale(Blockchain.Koinos.decimals())

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

        networkService.submitTransaction(signedTransaction)
            .successOr { return it.toSimpleResult() }

        // TODO add transaction to history ([REDACTED_TASK_KEY])

        return SimpleResult.Success
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        // TODO no fee child class?
        return Result.Success(TransactionFee.Single(normal = Fee.Common(amount = Amount(Blockchain.Koinos))))
    }
}