package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

internal class KoinosWalletManager(
    wallet: Wallet,
) : WalletManager(wallet = wallet) {
    override val currentHost: String
        get() = TODO()

    override suspend fun updateInternal() {
        TODO("Not yet implemented")
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        TODO("Not yet implemented")
    }
}