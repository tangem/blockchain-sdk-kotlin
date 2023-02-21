package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.extensions.Result

interface TronNetworkProvider {
    val network: TronNetwork

    suspend fun getAccount(address: String): Result<TronGetAccountResponse>

    suspend fun getAccountResource(address: String): Result<TronGetAccountResourceResponse>

    suspend fun getNowBlock(): Result<TronBlock>

    suspend fun broadcastHex(data: ByteArray): Result<TronBroadcastResponse>

    suspend fun getTokenBalance(
        address: String,
        contractAddress: String,
    ): Result<TronTriggerSmartContractResponse>

    suspend fun getTokenTransactionHistory(contractAddress: String): Result<TronTokenHistoryResponse>

    suspend fun getTransactionInfoById(id: String): Result<String>

    suspend fun getChainParameters() : Result<TronChainParametersResponse>
}
