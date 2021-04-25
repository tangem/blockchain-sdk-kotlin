package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class BitcoinNetworkService(private val providers: List<BitcoinNetworkProvider>) :
        MultiNetworkProvider<BitcoinNetworkProvider>(providers),
        BitcoinNetworkProvider {

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        val result = currentProvider.getInfo(address)
        return if (result.needsRetry()) getInfo(address) else result
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return coroutineScope {
            val resultsDeferred = providers.map { async { it.getFee() } }
            val results = resultsDeferred.map { it.await() }
            val fees = results.filterIsInstance<Result.Success<BitcoinFee>>().map { it.data }
            if (fees.isEmpty()) return@coroutineScope results.first()

            return@coroutineScope Result.Success(
                    BitcoinFee(
                            minimalPerKb =  fees.map { it.minimalPerKb }.maxOrNull()!!,
                            normalPerKb = fees.map { it.normalPerKb }.maxOrNull()!!,
                            priorityPerKb = fees.map { it.priorityPerKb }.maxOrNull()!!
                    )
            )
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = currentProvider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        val result = currentProvider.getSignatureCount(address)
        return if (result.needsRetry()) getSignatureCount(address) else result
    }
}