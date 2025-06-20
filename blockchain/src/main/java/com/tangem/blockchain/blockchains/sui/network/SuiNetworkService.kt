package com.tangem.blockchain.blockchains.sui.network

import com.tangem.blockchain.blockchains.sui.model.SuiCoin
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.COIN_TYPE
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiDryRunTransactionResponse
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiExecuteTransactionBlockResponse
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

internal class SuiNetworkService(
    providers: List<SuiJsonRpcProvider>,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getInfo(address: String): Result<SuiWalletInfo> {
        val response = coroutineScope {
            val firstRequest =
                async { multiJsonRpcProvider.performRequest { getCoins(address = address, cursor = null) } }
            val secondRequest =
                async {
                    multiJsonRpcProvider.performRequest {
                        getCoins(
                            address = address,
                            cursor = "eyJjb2luX3R5cGUiOiIweGYyMmRhOWEyNGFkMDI3Y2NjYjVmMmQ0OTZjYmU5MWRlOTUzZDM2MzUxM2RiMDhhM2E3MzRkMzYxYzdjMTc1MDM6OkxPRkk6OkxPRkkiLCJpbnZlcnRlZF9iYWxhbmNlIjoxODQ0Njc0NDA3MzcwOTU1MTYxNSwib2JqZWN0X2lkIjoiMHhlMTA2ODU5OWIyN2YxMGM1NmM4NjEyMTkzNjJjOGM1MWZhZDgwNDg2YjA3MjUwZmQ5NmM1OGI5MzZjZDhmZGNjIn0="
                        )
                    }
                }
            listOf(firstRequest, secondRequest).awaitAll().map { it.successOr { throw it.error } }
        }
        val dataList = response.flatMap { it.data }

        var totalSuiBalance = BigDecimal.ZERO
        val coins = mutableListOf<SuiCoin>()

        for (coin in dataList) {
            if (coin.coinType == COIN_TYPE) totalSuiBalance += coin.balance.movePointLeft(Blockchain.Sui.decimals())

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