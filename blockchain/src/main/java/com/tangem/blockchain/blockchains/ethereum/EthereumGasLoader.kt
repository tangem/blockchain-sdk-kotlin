package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.extensions.Result

interface EthereumGasLoader {
    suspend fun getGasPrice(): Result<Long>
    suspend fun getGasLimit(amount: Amount, destination: String): Result<Long>
}