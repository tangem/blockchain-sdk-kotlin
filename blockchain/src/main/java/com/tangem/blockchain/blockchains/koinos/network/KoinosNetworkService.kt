package com.tangem.blockchain.blockchains.koinos.network

import com.tangem.blockchain.blockchains.koinos.models.KoinosAccountInfo
import com.tangem.blockchain.blockchains.koinos.models.KoinosAccountNonce
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosProtocol
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider

internal class KoinosNetworkService(
    providers: List<KoinosApi>,
) : NetworkProvider {

    override val baseUrl: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    private val multiNetworkProvider = MultiNetworkProvider(providers)

    suspend fun getInfo(address: String): Result<KoinosAccountInfo> {
        val balance = multiNetworkProvider.performRequest(KoinosApi::getKoinBalance, address)
            .successOr { return it }
        val mana = multiNetworkProvider.performRequest(KoinosApi::getRC, address)
            .successOr { return it }

        return Result.Success(
            KoinosAccountInfo(
                koinBalance = balance.toBigDecimal().movePointLeft(Blockchain.Koinos.decimals()),
                mana = mana.toBigDecimal().movePointLeft(Blockchain.Koinos.decimals()),
            ),
        )
    }

    suspend fun getCurrentNonce(address: String): Result<KoinosAccountNonce> {
        return multiNetworkProvider.performRequest(KoinosApi::getNonce, address)
            .map { nonce -> KoinosAccountNonce(nonce) }
    }

    suspend fun submitTransaction(transaction: KoinosProtocol.Transaction, broadcast: Boolean): Result<Unit> {
        return multiNetworkProvider.performRequest(
            KoinosApi::submitTransaction,
            KoinosApi.TransactionRequest(transaction = transaction, broadcast = broadcast),
        )
    }
}