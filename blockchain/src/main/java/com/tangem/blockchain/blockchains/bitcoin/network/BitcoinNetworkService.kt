package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode

open class BitcoinNetworkService(
    providers: List<BitcoinNetworkProvider>,
    blockchain: Blockchain,
) : BitcoinNetworkProvider {

    private val multiProvider = MultiNetworkProvider(providers, blockchain)
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> =
        multiProvider.performRequest(BitcoinNetworkProvider::getInfo, address)

    override suspend fun getFee(): Result<BitcoinFee> = coroutineScope {
        val results = multiProvider.providers.map { async { it.getFee() } }.awaitAll()
        val fees = results.filterIsInstance<Result.Success<BitcoinFee>>().map { it.data }

        if (fees.isEmpty()) return@coroutineScope results.first { it is Result.Failure }

        Result.Success(fees.aggregateFee())
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult =
        multiProvider.performRequest(BitcoinNetworkProvider::sendTransaction, transaction)

    override suspend fun getSignatureCount(address: String): Result<Int> =
        multiProvider.performRequest(BitcoinNetworkProvider::getSignatureCount, address)

    override suspend fun getInfoByXpub(xpub: String): Result<XpubInfoResponse> =
        multiProvider.performRequest(BitcoinNetworkProvider::getInfoByXpub, xpub)

    override suspend fun getUtxoByXpub(xpub: String): Result<List<BitcoinUnspentOutput>> =
        multiProvider.performRequest(BitcoinNetworkProvider::getUtxoByXpub, xpub)

    private fun List<BitcoinFee>.aggregateFee(): BitcoinFee {
        if (size == 1) return first()

        return mergeFees { it.trimmedMean() }
    }

    private fun List<BitcoinFee>.mergeFees(aggregate: (List<BigDecimal>) -> BigDecimal) = BitcoinFee(
        minimalPerKb = aggregate(map { it.minimalPerKb }),
        normalPerKb = aggregate(map { it.normalPerKb }),
        priorityPerKb = aggregate(map { it.priorityPerKb }),
    )

    private fun List<BigDecimal>.trimmedMean(): BigDecimal {
        val values = if (size > 2) sorted().drop(1).dropLast(1) else this
        return values.reduce(BigDecimal::add)
            .divide(values.size.toBigDecimal(), Blockchain.Bitcoin.decimals(), RoundingMode.HALF_UP)
    }
}