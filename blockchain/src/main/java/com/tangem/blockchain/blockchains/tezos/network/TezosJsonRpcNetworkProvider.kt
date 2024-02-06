package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance

class TezosJsonRpcNetworkProvider(baseUrl: String) : TezosNetworkProvider {

    override val baseUrl: String = baseUrl

    private val api: TezosApi by lazy {
        createRetrofitInstance(baseUrl).create(TezosApi::class.java)
    }
    private val decimals = Blockchain.Tezos.decimals()

    override suspend fun getInfo(address: String): Result<TezosInfoResponse> {
        return try {
            val addressData = retryIO { api.getAddressData(address) }
            Result.Success(
                TezosInfoResponse(
                    balance = addressData.balance!!.toBigDecimal().movePointLeft(decimals),
                    counter = addressData.counter!!,
                ),
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun isPublicKeyRevealed(address: String): Result<Boolean> {
        return try {
            retryIO { api.getManagerKey(address) }
            Result.Success(true)
        } catch (exception: Exception) { // TODO: check exception
            Result.Success(false)
        }
    }

    override suspend fun getHeader(): Result<TezosHeader> {
        return try {
            val headerResponse = retryIO { api.getHeader() }
            Result.Success(
                TezosHeader(
                    hash = headerResponse.hash!!,
                    protocol = headerResponse.protocol!!,
                ),
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun forgeContents(forgeData: TezosForgeData): Result<String> {
        return try {
            val forgedContents = retryIO {
                api.forgeOperations(TezosForgeBody(forgeData.headerHash, forgeData.contents))
            }
            Result.Success(forgedContents)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun checkTransaction(transactionData: TezosTransactionData): SimpleResult {
        return try {
            val tezosPreapplyBody = TezosPreapplyBody(
                protocol = transactionData.header.protocol,
                branch = transactionData.header.hash,
                contents = transactionData.contents,
                signature = transactionData.encodedSignature,
            )
            retryIO { api.preapplyOperations(listOf(tezosPreapplyBody)) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }
}
