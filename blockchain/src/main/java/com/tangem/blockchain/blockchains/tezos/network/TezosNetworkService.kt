package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

interface TezosNetworkService {
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