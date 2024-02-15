package com.tangem.demo.accountcreator

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result

object DummyAccountCreator : AccountCreator {

    override suspend fun createAccount(blockchain: Blockchain, walletPublicKey: ByteArray): Result<String> =
        Result.Failure(BlockchainSdkError.WrappedThrowable(NotImplementedError("Dummy account creator")))
}