package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface TezosNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String): Result<TezosInfoResponse>
    suspend fun isPublicKeyRevealed(address: String): Result<Boolean>
    suspend fun getHeader(): Result<TezosHeader>
    suspend fun forgeContents(forgeData: TezosForgeData): Result<String>
    suspend fun checkTransaction(transactionData: TezosTransactionData): SimpleResult
    suspend fun sendTransaction(transaction: String): SimpleResult
}

data class TezosInfoResponse(
    val balance: BigDecimal,
    val counter: Long,
)

data class TezosHeader(
    val hash: String,
    val protocol: String,
)

data class TezosForgeData(
    val headerHash: String,
    val contents: List<TezosOperationContent>,
)

data class TezosTransactionData(
    val header: TezosHeader,
    val contents: List<TezosOperationContent>,
    val encodedSignature: String,
)
