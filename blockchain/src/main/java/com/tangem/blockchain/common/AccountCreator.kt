package com.tangem.blockchain.common

import com.tangem.blockchain.extensions.Result

interface AccountCreator {

    suspend fun createAccount(blockchain: Blockchain, walletPublicKey: ByteArray): Result<String>
}