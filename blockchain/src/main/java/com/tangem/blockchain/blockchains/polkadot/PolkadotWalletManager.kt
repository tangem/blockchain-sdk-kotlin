package com.tangem.blockchain.blockchains.polkadot

import android.util.Log
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.BlockchainSdkError.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.filterWith
import com.tangem.blockchain.extensions.successOr
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class PolkadotWalletManager(
    wallet: Wallet,
    jsonRpcProvider: RpcClient
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String = jsonRpcProvider.endpoint

    private val networkService = PolkadotNetworkService(jsonRpcProvider)

    private val feeRentHolder = mutableMapOf<Amount, BigDecimal>()

    override suspend fun update() {
        val accountInfo = networkService.getMainAccountInfo(accountPubK).successOr {
            cardTokens
            wallet.removeAllTokens()
            throw Exception(it.error)
        }
        wallet.setCoinValue(accountInfo.balance.toSOL())
        updateRecentTransactions()
        addToRecentTransactions(accountInfo.txsInProgress)

        cardTokens.forEach { cardToken ->
            val tokenBalance = accountInfo.tokensByMint[cardToken.contractAddress]?.uiAmount ?: BigDecimal.ZERO
            wallet.addTokenValue(tokenBalance, cardToken)
        }
    }

    private suspend fun updateRecentTransactions() {
//        val txSignatures = wallet.recentTransactions.mapNotNull { it.hash }
//        val signatureStatuses = networkService.getSignatureStatuses(txSignatures).successOr {
//            Log.e(this.javaClass.simpleName, it.error.localizedMessage ?: "Unknown error")
//            return
//        }
//
//        val confirmedTxData = mutableListOf<TransactionData>()
//        val signaturesStatuses = txSignatures.zip(signatureStatuses.value)
//        signaturesStatuses.forEach { pair ->
//            if (pair.second?.confirmationStatus == Commitment.FINALIZED.value) {
//                val foundRecentTxData = wallet.recentTransactions.firstOrNull { it.hash == pair.first }
//                foundRecentTxData?.let {
//                    confirmedTxData.add(it.copy(status = TransactionStatus.Confirmed))
//                }
//            }
//        }
//        updateRecentTransactions(confirmedTxData)
    }

    private fun addToRecentTransactions(txsInProgress: List<TransactionInfo>) {
//        if (txsInProgress.isEmpty()) return
//
//        val newTxsInProgress = txsInProgress.filterWith(wallet.recentTransactions) { a, b -> a.signature != b.hash }
//        val newUnconfirmedTxData = newTxsInProgress.mapNotNull {
//            if (it.instructions.isNotEmpty() && it.instructions[0].programId == Program.Id.system.toBase58()) {
//                val info = it.instructions[0].parsed.info
//                val amount = Amount(info.lamports.toSOL(), wallet.blockchain)
//                val fee = Amount(it.fee.toSOL(), wallet.blockchain)
//                TransactionData(amount, fee, info.source, info.destination, null,
//                    TransactionStatus.Unconfirmed, hash = it.signature)
//            } else {
//                null
//            }
//        }
//        wallet.recentTransactions.addAll(newUnconfirmedTxData)
    }

    override fun createTransaction(amount: Amount, fee: Amount, destination: String): TransactionData {
        val accountCreationRent = feeRentHolder[fee]

        return if (accountCreationRent == null) {
            super.createTransaction(amount, fee, destination)
        } else {
            when (amount.type) {
                AmountType.Coin -> {
                    val newFee = fee.minus(accountCreationRent)
                    val newAmount = amount.plus(accountCreationRent)
                    super.createTransaction(newAmount, newFee, destination)
                }
                is AmountType.Token -> throw UnsupportedOperation()
                AmountType.Reserve -> throw UnsupportedOperation()
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer)
            is AmountType.Token -> throw UnsupportedOperation()
            AmountType.Reserve -> SimpleResult.Failure(UnsupportedOperation())
        }
    }

    private suspend fun sendCoin(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return SimpleResult.Success
    }


    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
    }

}