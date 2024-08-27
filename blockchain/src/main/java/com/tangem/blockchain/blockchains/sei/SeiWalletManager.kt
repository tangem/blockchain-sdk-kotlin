package com.tangem.blockchain.blockchains.sei

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result

class SeiWalletManager(
    wallet: Wallet,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String get() = TODO("[REDACTED_JIRA]")

    override suspend fun updateInternal() {
        TODO("[REDACTED_JIRA]")
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        TODO("[REDACTED_JIRA]")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        TODO("[REDACTED_JIRA]")
    }
}