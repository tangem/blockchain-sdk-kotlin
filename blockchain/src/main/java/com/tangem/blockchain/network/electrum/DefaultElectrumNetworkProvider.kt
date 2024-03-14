package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.electrum.api.ElectrumApiService
import com.tangem.blockchain.network.electrum.api.WebSocketElectrumApiService
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultElectrumNetworkProvider(
    override val baseUrl: String,
    private val blockchain: Blockchain,
) : ElectrumNetworkProvider {

    private val service: WebSocketElectrumApiService = WebSocketElectrumApiService(
        wssUrl = baseUrl,
        okHttpClient = OkHttpClient(),
    )

    private val serverVersionRequested = AtomicBoolean(false)

    override suspend fun getAccount(addressScriptHash: String): Result<ElectrumAccount> {
        firstCheckServer()?.apply { return Result.Failure(this) }

        return retryCall {
            service.getBalance(addressScriptHash)
        }.map {
            ElectrumAccount(
                confirmedAmount = it.satoshiConfirmed.toBigDecimal().movePointLeft(blockchain.decimals()),
                unconfirmedAmount = it.satoshiUnconfirmed.toBigDecimal().movePointLeft(blockchain.decimals()),
            )
        }
    }

    override suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumUnspentUTXORecord>> {
        firstCheckServer()?.apply { return Result.Failure(this) }

        return retryCall {
            service.getUnspentUTXOs(addressScriptHash)
        }.map { list ->
            list.map {
                ElectrumUnspentUTXORecord(
                    height = it.height,
                    txPos = it.txPos,
                    txHash = it.txHash,
                    value = it.valueSatoshi.toBigDecimal().movePointLeft(blockchain.decimals()),
                    outpointHash = it.outpointHash,
                )
            }
        }
    }

    // TODO
    // override suspend fun getTransaction(txHash: String): Result<ElectrumTransaction> {
    //     firstCheckServer()?.apply { return Result.Failure(this) }
    //
    //     return retryCall {
    //         service.getTransaction(txHash)
    //     }.map {
    //         val vout = it.vout.firstNotNullOfOrNull {
    //             it.scriptPublicKey.scriptHash
    //         } ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)
    //
    //         ElectrumTransaction(
    //             hash = vout
    //         )
    //     }
    // }

    override suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumEstimateFee> {
        firstCheckServer()?.apply { return Result.Failure(this) }

        return retryCall {
            service.getEstimateFee(numberConfirmationBlocks)
        }.map {
            ElectrumEstimateFee(it.feeInCoinsPer1000Bytes)
        }
    }

    private suspend fun firstCheckServer(): BlockchainError? {
        if (serverVersionRequested.get()) {
            return null
        }

        return when (val serverInfo = service.getServerVersion()) {
            is Result.Success -> {
                if (serverInfo.data.versionNumber == ElectrumApiService.SUPPORTED_PROTOCOL_VERSION) {
                    serverVersionRequested.set(true)
                    null
                } else { // node doesn't support requested electrum protocol version
                    BlockchainSdkError.UnsupportedOperation(
                        """
                            Expected protocol version: ${ElectrumApiService.SUPPORTED_PROTOCOL_VERSION}"
                            Protocol version supported by server: ${serverInfo.data.versionNumber}
                        """.trimIndent(),
                    )
                }
            }
            is Result.Failure -> {
                serverInfo.error
            }
        }
    }

    private suspend fun <T> retryCall(
        times: Int = 10,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> Result<T>,
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(times - 1) {
            val res = block()

            if (res is Result.Failure && res.error is BlockchainSdkError.ElectrumBlockchain.Api) {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            } else {
                return res
            }
        }
        return block() // last attempt
    }
}