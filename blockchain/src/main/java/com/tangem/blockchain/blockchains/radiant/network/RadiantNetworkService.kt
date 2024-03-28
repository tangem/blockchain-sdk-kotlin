package com.tangem.blockchain.blockchains.radiant.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.radiant.models.RadiantAccountInfo
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class RadiantNetworkService(providers: List<ElectrumNetworkProvider>) : NetworkProvider {

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers)

    private val defaultFeePer1000Bytes = "0.00001".toBigDecimal()
    private val normalFeeMultiplier = "1.5".toBigDecimal()
    private val priorityFeeMultiplier = 2.toBigDecimal()

    suspend fun getInfo(address: String, scriptHash: String): Result<RadiantAccountInfo> {
        return coroutineScope {
            val balance = multiProvider.performRequest(ElectrumNetworkProvider::getAccountBalance, scriptHash)
                .successOr { return@coroutineScope it }
            val unspentOutputs = getUnspentOutputs(address = address, scriptHash = scriptHash)
                .successOr { return@coroutineScope it }
            Result.Success(
                RadiantAccountInfo(
                    balance = balance.confirmedAmount,
                    unspentOutputs = unspentOutputs,
                ),
            )
        }
    }

    suspend fun getEstimatedFee(numberOfBlocks: Int): Result<BitcoinFee> {
        return multiProvider.performRequest(ElectrumNetworkProvider::getEstimateFee, numberOfBlocks)
            .map { feeResponse ->
                val feePer1000Bytes = feeResponse.feeInCoinsPer1000Bytes ?: defaultFeePer1000Bytes
                BitcoinFee(
                    minimalPerKb = feePer1000Bytes,
                    normalPerKb = feePer1000Bytes.multiply(normalFeeMultiplier),
                    priorityPerKb = feePer1000Bytes.multiply(priorityFeeMultiplier),
                )
            }
    }

    private suspend fun getUnspentOutputs(address: String, scriptHash: String): Result<List<BitcoinUnspentOutput>> {
        val utxoRecords = multiProvider
            .performRequest(ElectrumNetworkProvider::getUnspentUTXOs, scriptHash)
            .successOr { return it }
        val scripts = coroutineScope {
            utxoRecords.map { utxoRecord ->
                async {
                    multiProvider.performRequest(ElectrumNetworkProvider::getTransactionInfo, utxoRecord.txHash)
                }
            }
        }
            .awaitAll()
            .map { it.successOr { error -> return error } }
        val unspents = utxoRecords.mapNotNull { utxoRecord ->
            val vout = scripts
                .firstOrNull { it.hash.equals(utxoRecord.txHash, ignoreCase = true) }
                ?.vout
                ?.firstOrNull { it.scriptPublicKey?.addresses?.contains(address) == true }
                .guard { return@mapNotNull null }
            val scriptPublicKey = vout.scriptPublicKey ?: return@mapNotNull null

            BitcoinUnspentOutput(
                amount = utxoRecord.value,
                outputIndex = utxoRecord.txPos,
                transactionHash = utxoRecord.txHash.hexToBytes(),
                outputScript = scriptPublicKey.hex.hexToBytes(),
            )
        }
        return Result.Success(unspents)
    }

    internal companion object {
        const val SUPPORTED_SERVER_VERSION = "1.4"
    }
}