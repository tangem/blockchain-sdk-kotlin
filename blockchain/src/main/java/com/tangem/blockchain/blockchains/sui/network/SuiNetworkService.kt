package com.tangem.blockchain.blockchains.sui.network

import com.tangem.blockchain.blockchains.sui.model.SuiDryRunResult
import com.tangem.blockchain.blockchains.sui.model.SuiExecuteResult
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.COIN_TYPE
import com.tangem.blockchain.blockchains.sui.network.grpc.SuiGrpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class SuiNetworkService(
    providers: List<SuiGrpcProvider>,
    blockchain: Blockchain,
) {

    private val multiProvider = MultiNetworkProvider(providers, blockchain)

    val host: String get() = multiProvider.currentProvider.baseUrl

    suspend fun getInfo(address: String): Result<SuiWalletInfo> {
        val coins = multiProvider
            .performRequest(SuiGrpcProvider::getCoins, address)
            .successOr { return it }

        val totalSuiBalance = coins
            .filter { it.coinType == COIN_TYPE }
            .sumOf { it.mistBalance }
            .movePointLeft(Blockchain.Sui.decimals())

        return Result.Success(SuiWalletInfo(suiTotalBalance = totalSuiBalance, coins = coins))
    }

    suspend fun getReferenceGasPrice(): Result<BigDecimal> {
        return multiProvider.performRequest(SuiGrpcProvider::getReferenceGasPrice)
    }

    suspend fun dryRunTransaction(transactionBytes: String): Result<SuiDryRunResult> {
        return multiProvider.performRequest(SuiGrpcProvider::dryRunTransaction, transactionBytes)
    }

    suspend fun executeTransaction(transactionBytes: String, signature: String): Result<SuiExecuteResult> {
        return multiProvider.performRequest { executeTransaction(transactionBytes, signature) }
    }

    suspend fun isTransactionConfirmed(transactionHash: String): Boolean {
        multiProvider
            .performRequest(SuiGrpcProvider::getTransaction, transactionHash)
            .successOr { return false }

        return true
    }
}