package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiProvider
import java.math.BigDecimal


class BitcoinNetworkManager(providers: List<BitcoinNetworkService>) :
        MultiProvider<BitcoinNetworkService>(providers),
        BitcoinNetworkService {

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        val result = provider.getInfo(address)
        return if (result.needsRetry()) getInfo(address) else result
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        val result = provider.getFee()
        return if (result.needsRetry()) getFee() else result
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = provider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        val result = provider.getSignatureCount(address)
        return if (result.needsRetry()) getSignatureCount(address) else result
    }
}

data class BitcoinAddressInfo(
        val balance: BigDecimal,
        val unspentOutputs: List<BitcoinUnspentOutput>,
        val recentTransactions: List<BasicTransactionData>,
        val hasUnconfirmed: Boolean? = null //additional logic for when recent transactions are absent or not reliable
)

data class BitcoinFee(
        val minimalPerKb: BigDecimal,
        val normalPerKb: BigDecimal,
        val priorityPerKb: BigDecimal
)