package com.tangem.blockchain.blockchains.icp

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result

internal class InternetComputerWalletManager(
    wallet: Wallet,
) : WalletManager(wallet) {

    override val currentHost: String = ""

    override suspend fun updateInternal() {
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return Result.Failure(BlockchainSdkError.FailedToSendException)
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return Result.Failure(BlockchainSdkError.FailedToLoadFee)
    }
}
