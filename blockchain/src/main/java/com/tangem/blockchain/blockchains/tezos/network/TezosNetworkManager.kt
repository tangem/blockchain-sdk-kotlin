package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_TEZOS
import com.tangem.blockchain.network.API_TEZOS_RESERVE
import com.tangem.blockchain.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal

class TezosNetworkManager : TezosNetworkService {
    private val tezosProvider by lazy {
        val api = createRetrofitInstance(API_TEZOS)
                .create(TezosApi::class.java)
        TezosProvider(api)
    }

    private val tezosReserveProvider by lazy {
        val api = createRetrofitInstance(API_TEZOS_RESERVE)
                .create(TezosApi::class.java)
        TezosProvider(api)
    }

    var provider = tezosProvider

    private fun changeProvider() {
        provider = if (provider == tezosProvider) tezosReserveProvider else tezosProvider
    }

    override suspend fun getInfo(address: String): Result<TezosInfoResponse> {
        return when (val result = provider.getInfo(address)) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.getInfo(address)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun isPublicKeyRevealed(address: String): Result<Boolean> {
        return when (val result = provider.isPublicKeyRevealed(address)) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.isPublicKeyRevealed(address)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun getHeader(): Result<TezosHeader> {
        return when (val result = provider.getHeader()) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.getHeader()
                } else {
                    result
                }
            }
        }
    }

    override suspend fun forgeContents(headerHash: String, contents: List<TezosOperationContent>): Result<String> {
        return when (val result = provider.forgeContents(headerHash, contents)) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.forgeContents(headerHash, contents)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun checkTransaction(
            header: TezosHeader,
            contents: List<TezosOperationContent>,
            signature: ByteArray
    ): SimpleResult {
        return when (val result = provider.checkTransaction(header, contents, signature)) {
            is SimpleResult.Success -> result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.checkTransaction(header, contents, signature)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return when (val result = provider.sendTransaction(transaction)) {
            is SimpleResult.Success -> result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.sendTransaction(transaction)
                } else {
                    result
                }
            }
        }
    }
}

data class TezosInfoResponse(
        val balance: BigDecimal,
        val counter: Long
)

data class TezosHeader(
        val hash: String,
        val protocol: String
)