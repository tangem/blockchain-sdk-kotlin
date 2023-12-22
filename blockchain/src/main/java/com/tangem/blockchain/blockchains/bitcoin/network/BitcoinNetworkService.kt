package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode

open class BitcoinNetworkService(providers: List<BitcoinNetworkProvider>) : BitcoinNetworkProvider {

    private val multiProvider = MultiNetworkProvider(providers)
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> =
        multiProvider.performRequest(BitcoinNetworkProvider::getInfo, address)

    override suspend fun getFee(): Result<BitcoinFee> {
        return coroutineScope {
            val resultsDeferred = multiProvider.providers.map { async { it.getFee() } }
            val results = resultsDeferred.map { it.await() }
            val fees = results.filterIsInstance<Result.Success<BitcoinFee>>().map { it.data }
            if (fees.isEmpty()) return@coroutineScope results.first()

            val bitcoinFee = if (fees.size > 2) {
                BitcoinFee(
                    minimalPerKb = fees.map { it.minimalPerKb }.sorted().drop(1).average(),
                    normalPerKb = fees.map { it.normalPerKb }.sorted().drop(1).average(),
                    priorityPerKb = fees.map { it.priorityPerKb }.sorted().drop(1).average(),
                )
            } else {
                BitcoinFee(
                    minimalPerKb = fees.map { it.minimalPerKb }.maxOrNull()!!,
                    normalPerKb = fees.map { it.normalPerKb }.maxOrNull()!!,
                    priorityPerKb = fees.map { it.priorityPerKb }.maxOrNull()!!,
                )
            }

            Result.Success(bitcoinFee)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult =
        multiProvider.performRequest(BitcoinNetworkProvider::sendTransaction, transaction)

    override suspend fun getSignatureCount(address: String): Result<Int> =
        multiProvider.performRequest(BitcoinNetworkProvider::getSignatureCount, address)

    private fun List<BigDecimal>.average(): BigDecimal =
        this.reduce { acc, number -> acc + number }.divide(this.size.toBigDecimal(), RoundingMode.HALF_UP)
            .setScale(Blockchain.Bitcoin.decimals(), RoundingMode.HALF_UP)
}
