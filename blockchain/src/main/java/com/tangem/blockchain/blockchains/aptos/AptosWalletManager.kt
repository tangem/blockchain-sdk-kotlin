package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

class AptosWalletManager(wallet: Wallet) : WalletManager(wallet), TransactionSender {

    override val currentHost: String get() = TODO("https://tangem.atlassian.net/browse/AND-5824")

    override suspend fun updateInternal() {
        TODO("https://tangem.atlassian.net/browse/AND-5824")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        TODO("https://tangem.atlassian.net/browse/AND-5824")
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        TODO("https://tangem.atlassian.net/browse/AND-5824")
    }
}
