package com.tangem.blockchain.blockchains.sui.network

import com.tangem.blockchain.blockchains.sui.model.SuiCoin
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.COIN_TYPE
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiDryRunTransactionResponse
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiExecuteTransactionBlockResponse
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class SuiNetworkService(
    providers: List<SuiJsonRpcProvider>,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getInfo(address: String): Result<SuiWalletInfo> {
        val response = multiJsonRpcProvider
            .performRequest(SuiJsonRpcProvider::getCoins, address)
            .successOr { return it }

        var totalSuiBalance = BigDecimal.ZERO
        val coins = mutableListOf<SuiCoin>()

        for (coin in response.data) {
            if (coin.coinType == COIN_TYPE) totalSuiBalance += coin.balance.movePointLeft(SuiConstants.MIST_SCALE)

            val suiCoin = SuiCoin(
                objectId = coin.coinObjectId,
                coinType = coin.coinType,
                mistBalance = coin.balance,
                version = coin.version.toLong(),
                digest = coin.digest,
            )
            coins.add(suiCoin)
        }

        val info = SuiWalletInfo(
            suiTotalBalance = totalSuiBalance,
            coins = coins,
        )

        return Result.Success(info)
    }

    suspend fun getReferenceGasPrice(): Result<BigDecimal> {
        return multiJsonRpcProvider.performRequest(SuiJsonRpcProvider::getReferenceGasPrice)
    }

    suspend fun dryRunTransaction(transactionHash: String): Result<SuiDryRunTransactionResponse> {
        val response = multiJsonRpcProvider
            .performRequest(SuiJsonRpcProvider::dryRunTransaction, transactionHash)
            .successOr { return it }

        return Result.Success(response)
    }

    suspend fun executeTransaction(
        transactionHash: String,
        signature: String,
    ): Result<SuiExecuteTransactionBlockResponse> {
        val response = multiJsonRpcProvider
            .performRequest { executeTransaction(transactionHash, signature) }
            .successOr { return it }

        return Result.Success(response)
    }

    suspend fun isTransactionConfirmed(transactionHash: String): Boolean {
        multiJsonRpcProvider
            .performRequest(SuiJsonRpcProvider::getTransactionBlock, transactionHash)
            .successOr { return false }

        return true
    }
}