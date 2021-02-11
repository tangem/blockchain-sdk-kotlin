package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface TezosNetworkProvider {
    suspend fun getInfo(address: String): Result<TezosInfoResponse>
    suspend fun isPublicKeyRevealed(address: String): Result<Boolean>
    suspend fun getHeader(): Result<TezosHeader>
    suspend fun forgeContents(headerHash: String, contents: List<TezosOperationContent>): Result<String>
    suspend fun checkTransaction(
            header: TezosHeader,
            contents: List<TezosOperationContent>,
            signature: ByteArray
    ): SimpleResult

    suspend fun sendTransaction(transaction: String): SimpleResult
}

data class TezosInfoResponse(
        val balance: BigDecimal,
        val counter: Long
)

data class TezosHeader(
        val hash: String,
        val protocol: String
)