package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

interface EthereumGasLoader {
    suspend fun getGasPrice(): Result<BigInteger>
    suspend fun getGasLimit(amount: Amount, destination: String): Result<BigInteger>
    suspend fun getGasLimit(amount: Amount, destination: String, callData: SmartContractCallData): Result<BigInteger>
}