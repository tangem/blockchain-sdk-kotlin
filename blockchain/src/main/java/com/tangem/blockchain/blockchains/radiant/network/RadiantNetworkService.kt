package com.tangem.blockchain.blockchains.radiant.network

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider

internal class RadiantNetworkService(providers: List<ElectrumNetworkProvider>) : NetworkProvider {

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers)

    private val defaultFeePer1000Bytes = "0.00001".toBigDecimal()
    private val normalFeeMultiplier = "1.5".toBigDecimal()
    private val priorityFeeMultiplier = 2.toBigDecimal()

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

    internal companion object {
        const val SUPPORTED_SERVER_VERSION = "1.4"
    }
}