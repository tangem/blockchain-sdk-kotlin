package com.tangem.blockchain.blockchains.kaspa.krc20

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

open class KaspaKRC20RestApiNetworkProvider(override val baseUrl: String) : KaspaKRC20NetworkProvider {

    private val api: KaspaKRC20Api by lazy {
        createRetrofitInstance(baseUrl).create(KaspaKRC20Api::class.java)
    }
    private val decimals = Blockchain.Kaspa.decimals()

    override suspend fun getBalances(
        address: String,
        tokens: List<Token>,
    ): Result<Map<Token, Result<KaspaKRC20InfoResponse>>> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.associateWith { token ->
                async {
                    retryIO {
                        try {
                            val response = api.getBalance(address, token.contractAddress).result.first()
                            Result.Success(
                                KaspaKRC20InfoResponse(
                                    balance = response.balance!!.toBigDecimal().movePointLeft(decimals),
                                ),
                            )
                        } catch (e: Exception) {
                            Result.Failure(e.toBlockchainSdkError())
                        }
                    }
                }
            }

            Result.Success(
                tokenBalancesDeferred.mapValues { it.value.await() },
            )
        }
    }
}