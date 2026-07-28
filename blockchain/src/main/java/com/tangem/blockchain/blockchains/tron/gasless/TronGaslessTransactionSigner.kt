package com.tangem.blockchain.blockchains.tron.gasless

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result

/**
 * Builds + signs (one signing session, no broadcast) the TRC-20 transfers of a Tron gasless flow,
 * returning each as a TronWeb-compatible signed-JSON string in input order.
 * Implemented by [com.tangem.blockchain.blockchains.tron.TronWalletManager]; the app reaches it by
 * casting the wallet manager.
 */
interface TronGaslessTransactionSigner {
    suspend fun signGaslessTransactions(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<List<String>>
}