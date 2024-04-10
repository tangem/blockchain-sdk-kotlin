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
// [REDACTED_TODO_COMMENT]
        wallet.additionalInfo = WalletAdditionalInfo.Koinos(
            mana = accountInfo.mana,
            timeToChargeMillis = 0L,
        )
// [REDACTED_TODO_COMMENT]
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val nonce = networkService.getCurrentNonce(wallet.address)
            .successOr { return SimpleResult.fromTangemSdkError(it.error) }
// [REDACTED_TODO_COMMENT]
        val testManaLimit = (wallet.additionalInfo as WalletAdditionalInfo.Koinos)
            .mana.multiply(0.1.toBigDecimal())
            .setScale(Blockchain.Koinos.decimals())
// [REDACTED_TODO_COMMENT]
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
// [REDACTED_TODO_COMMENT]

        return SimpleResult.Success
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
// [REDACTED_TODO_COMMENT]
        return Result.Success(TransactionFee.Single(normal = Fee.Common(amount = Amount(Blockchain.Koinos))))
    }
}
