package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.extensions.Result
import org.kethereum.model.Transaction
import java.math.BigInteger

interface EthereumGasLoader {
    suspend fun getGasPrice(): Result<BigInteger>
    suspend fun getGasLimit(amount: Amount, destination: String): Result<BigInteger>
    suspend fun getGasLimit(amount: Amount, destination: String, data: String): Result<BigInteger>
}

data class CompiledEthereumTransaction(
    val transaction: Transaction,
    val hash: ByteArray,
)

data class SignedEthereumTransaction(
    val compiledTransaction: CompiledEthereumTransaction,
    val signature: ByteArray,
)
