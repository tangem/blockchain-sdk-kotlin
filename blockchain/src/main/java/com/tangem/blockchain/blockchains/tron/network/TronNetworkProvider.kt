package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

interface TronNetworkProvider : NetworkProvider {
    val network: TronNetwork

    suspend fun getAccount(address: String): Result<TronGetAccountResponse>

    suspend fun getAccountResource(address: String): Result<TronGetAccountResourceResponse>

    suspend fun getNowBlock(): Result<TronBlock>

    suspend fun broadcastHex(data: ByteArray): Result<TronBroadcastResponse>

    suspend fun getTokenBalance(
        tokenBalanceRequestData: TokenBalanceRequestData,
    ): Result<TronTriggerSmartContractResponse>

    suspend fun contractEnergyUsage(
        address: String,
        contractAddress: String,
        parameter: String,
    ): Result<TronTriggerSmartContractResponse>

    suspend fun getTransactionInfoById(id: String): Result<String>

    suspend fun getChainParameters(): Result<TronChainParametersResponse>
}
