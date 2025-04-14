package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.electrum.api.ElectrumApiService
import com.tangem.blockchain.network.electrum.api.ElectrumResponse
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

internal class DefaultElectrumNetworkProvider(
    override val baseUrl: String,
    private val blockchain: Blockchain,
    private val service: ElectrumApiService,
    private val supportedProtocolVersion: String,
) : ElectrumNetworkProvider {

    private val serverVersionRequested = MutableStateFlow<VersionRequestState>(VersionRequestState.NotRequested)

    override suspend fun getAccountBalance(addressScriptHash: String): Result<ElectrumAccount> {
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

    override suspend fun getTransactionHistory(
        addressScriptHash: String,
    ): Result<List<ElectrumResponse.TxHistoryEntry>> {
        firstCheckServer()?.apply { return Result.Failure(this) }

        return retryCall {
            service.getTransactionHistory(addressScriptHash = addressScriptHash)
        }
    }

    override suspend fun getTransactionInfo(txHash: String): Result<ElectrumResponse.Transaction> {
        firstCheckServer()?.apply { return Result.Failure(this) }

        return retryCall {
            service.getTransaction(txHash = txHash)
        }
    }

    override suspend fun broadcastTransaction(rawTx: ByteArray): Result<ElectrumResponse.TxHex> {
        firstCheckServer()?.apply { return Result.Failure(this) }

        return retryCall {
            service.sendTransaction(rawTransactionHex = rawTx.toHexString())
        }
    }

    private suspend fun firstCheckServer(): BlockchainError? {
        return when (val state = serverVersionRequested.value) {
            VersionRequestState.NotRequested -> getServerVersion()
            is VersionRequestState.Requested -> if (state.result is SimpleResult.Success) null else getServerVersion()
            VersionRequestState.Requesting -> {
                val requestedState = serverVersionRequested
                    .filterIsInstance<VersionRequestState.Requested>()
                    .first()
                if (requestedState.result is SimpleResult.Success) null else getServerVersion()
            }
        }
    }

    private suspend fun getServerVersion(): BlockchainError? {
        serverVersionRequested.value = VersionRequestState.Requesting
        val serverInfo = service.getServerVersion(supportedProtocolVersion = supportedProtocolVersion)

        val error: BlockchainError? = when (serverInfo) {
            is Result.Success -> {
                if (serverInfo.data.versionNumber == supportedProtocolVersion) {
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
        val requestedState = if (error == null) SimpleResult.Success else SimpleResult.Failure(error)
        serverVersionRequested.value = VersionRequestState.Requested(requestedState)
        return error
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

    private sealed interface VersionRequestState {
        data object NotRequested : VersionRequestState
        data object Requesting : VersionRequestState
        data class Requested(val result: SimpleResult) : VersionRequestState
    }
}