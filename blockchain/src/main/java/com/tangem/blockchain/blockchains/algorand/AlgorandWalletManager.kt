package com.tangem.blockchain.blockchains.algorand

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

class AlgorandWalletManager(wallet: Wallet) : WalletManager(wallet) {
    override val currentHost: String
        get() = TODO("Not yet implemented")

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