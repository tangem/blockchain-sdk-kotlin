package com.tangem.blockchain.blockchains.hedera.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

internal class HederaMirrorRestProvider(override val baseUrl: String, key: String? = null) : HederaNetworkProvider {

    private val api: HederaMirrorNodeApi by lazy {
        createRetrofitInstance(
            baseUrl = baseUrl,
            headerInterceptors = listOf(
                AddHeaderInterceptor(
                    headers = buildMap {
                        key?.let { put("X-API-Key", it) }
                    },
                ),
            ),
        ).create(HederaMirrorNodeApi::class.java)
    }

    override suspend fun getAccountId(publicKey: ByteArray): Result<String> {
        return try {
            val accountData = retryIO { api.getAccountsByPublicKey(publicKey.toHexString()) }
            if (accountData.accounts.size == 1) {
                Result.Success(accountData.accounts[0].account) // we expect only one account per public key
            } else {
                Result.Failure(BlockchainSdkError.AccountNotFound())
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getUsdExchangeRate(): Result<BigDecimal> {
        return try {
            val exchangeRateData = retryIO { api.getExchangeRate() }
            // use current rate for simplicity, we make an overhead on transaction building anyway
            val rateToUse = exchangeRateData.currentRate
            Result.Success(
                CENTS_IN_A_DOLLAR.toBigDecimal().setScale(Blockchain.Hedera.decimals()) *
                    rateToUse.hbarEquivalent.toBigDecimal() /
                    rateToUse.centEquivalent.toBigDecimal(),
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getBalances(accountId: String): Result<HederaBalancesResponse> {
        return try {
            val balances = retryIO { api.getBalances(accountId) }
            Result.Success(balances)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getTransactionInfo(transactionId: String): Result<HederaTransactionResponse> {
        return try {
            val response = retryIO { api.getTransactionInfo(transactionId) }
            return Result.Success(requireNotNull(response.transactions.firstOrNull()))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getTokenDetails(tokenId: String): Result<HederaTokenDetailsResponse> {
        return try {
            val response = retryIO { api.getTokenDetails(tokenId) }
            return Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    companion object {
        private const val CENTS_IN_A_DOLLAR = 100
    }
}