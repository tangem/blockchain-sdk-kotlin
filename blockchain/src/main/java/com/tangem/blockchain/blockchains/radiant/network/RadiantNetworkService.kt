package com.tangem.blockchain.blockchains.radiant.network

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.radiant.models.RadiantAccountInfo
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

internal class RadiantNetworkService(providers: List<ElectrumNetworkProvider>) : NetworkProvider {

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers)

    /**
     * Relying on answers from blockchain developers and costs from the official application (Electron-Radiant).
     * 10000 satoshi per byte or 0.1 RXD per KB.
     */
    private val recommendedFeePer1000Bytes = "0.1".toBigDecimal()
    private val normalFeeMultiplier = "1.5".toBigDecimal()
    private val priorityFeeMultiplier = 2.toBigDecimal()

    suspend fun getInfo(scriptHash: String): Result<RadiantAccountInfo> {
        return coroutineScope {
            val balance = multiProvider.performRequest(ElectrumNetworkProvider::getAccountBalance, scriptHash)
                .successOr { return@coroutineScope it }
            val unspents = multiProvider.performRequest(ElectrumNetworkProvider::getUnspentUTXOs, scriptHash)
                .successOr { return@coroutineScope it }

            Result.Success(RadiantAccountInfo(balance = balance.confirmedAmount, unspentOutputs = unspents))
        }
    }

    suspend fun getEstimatedFee(numberOfBlocks: Int): Result<BitcoinFee> {
        return multiProvider.performRequest(ElectrumNetworkProvider::getEstimateFee, numberOfBlocks)
            .map { feeResponse ->
                val sourceFee = feeResponse.feeInCoinsPer1000Bytes ?: BigDecimal.ZERO

                // We will only use the server response if it is greater than the recommended one
                val targetFee = if (sourceFee > recommendedFeePer1000Bytes) sourceFee else recommendedFeePer1000Bytes
                BitcoinFee(
                    minimalPerKb = targetFee,
                    normalPerKb = targetFee.multiply(normalFeeMultiplier),
                    priorityPerKb = targetFee.multiply(priorityFeeMultiplier),
                )
            }
    }

    suspend fun sendTransaction(rawTx: ByteArray): Result<String> {
        return multiProvider.performRequest(ElectrumNetworkProvider::broadcastTransaction, rawTx).map { it.hash }
    }

    internal companion object {
        const val SUPPORTED_SERVER_VERSION = "1.4"
    }
}