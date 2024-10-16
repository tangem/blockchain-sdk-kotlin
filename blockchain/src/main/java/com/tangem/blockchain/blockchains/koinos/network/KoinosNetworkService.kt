package com.tangem.blockchain.blockchains.koinos.network

import com.tangem.blockchain.blockchains.koinos.models.KoinosAccountInfo
import com.tangem.blockchain.blockchains.koinos.models.KoinosAccountNonce
import com.tangem.blockchain.blockchains.koinos.models.KoinosTransactionEntry
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosProtocol
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class KoinosNetworkService(
    providers: List<KoinosNetworkProvier>,
) : NetworkProvider {

    override val baseUrl: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    private val multiNetworkProvider = MultiNetworkProvider(providers)

    suspend fun getInfo(address: String): Result<KoinosAccountInfo> {
        val balance = multiNetworkProvider.performRequest(KoinosNetworkProvier::getKoinBalance, address)
            .successOr { return it }
        val mana = multiNetworkProvider.performRequest(KoinosNetworkProvier::getRC, address)
            .successOr { return it }

        val balanceDecimal = balance.toBigDecimal().movePointLeft(Blockchain.Koinos.decimals())
        val manaDecimal = mana.toBigDecimal().movePointLeft(Blockchain.Koinos.decimals())

        return Result.Success(
            KoinosAccountInfo(
                koinBalance = balanceDecimal,
                mana = manaDecimal,
                maxMana = balanceDecimal,
            ),
        )
    }

    suspend fun getCurrentNonce(address: String): Result<KoinosAccountNonce> {
        return multiNetworkProvider.performRequest(KoinosNetworkProvier::getNonce, address)
            .map { nonce -> KoinosAccountNonce(nonce) }
    }

    suspend fun submitTransaction(transaction: KoinosProtocol.Transaction): Result<KoinosTransactionEntry> {
        return multiNetworkProvider.performRequest(KoinosNetworkProvier::submitTransaction, transaction)
    }

    suspend fun getTransactionHistory(
        address: String,
        pageSize: Int,
        sequenceNum: Long,
    ): Result<List<KoinosTransactionEntry>> {
        return multiNetworkProvider.performRequest(
            KoinosNetworkProvier::getTransactionHistory,
            TransactionHistoryRequest(
                address = address,
                pageSize = pageSize,
                sequenceNum = sequenceNum,
            ),
        )
    }

    suspend fun getRCLimit(): Result<BigDecimal> {
        val limits = multiNetworkProvider.performRequest(KoinosNetworkProvier::getResourceLimits)
            .successOr { return it }

        val rcLimitSatoshi = MAX_DISK_STORAGE_LIMIT * limits.diskStorageCost +
            MAX_NETWORK_LIMIT * limits.networkBandwidthCost +
            MAX_COMPUTE_LIMIT * limits.computeBandwidthCost

        return Result.Success(
            BigDecimal(rcLimitSatoshi).movePointLeft(Blockchain.Koinos.decimals()),
        )
    }

    private companion object {
        const val MAX_DISK_STORAGE_LIMIT = 118
        const val MAX_NETWORK_LIMIT = 408
        const val MAX_COMPUTE_LIMIT = 1_000_000
    }
}
