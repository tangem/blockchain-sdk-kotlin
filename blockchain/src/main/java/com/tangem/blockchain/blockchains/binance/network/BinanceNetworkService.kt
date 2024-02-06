package com.tangem.blockchain.blockchains.binance.network

import com.tangem.blockchain.blockchains.binance.client.BinanceDexApiClientFactory
import com.tangem.blockchain.blockchains.binance.client.BinanceDexApiRestClient
import com.tangem.blockchain.blockchains.binance.client.BinanceDexEnvironment
import com.tangem.blockchain.blockchains.binance.client.encoding.message.TransactionRequestAssemblerExtSign
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BINANCE
import com.tangem.blockchain.network.API_BINANCE_TESTNET
import com.tangem.blockchain.network.createRetrofitInstance
import java.math.BigDecimal

class BinanceNetworkService(isTestNet: Boolean = false) : BinanceNetworkProvider {
    override val host: String = if (!isTestNet) API_BINANCE else API_BINANCE_TESTNET

    private val api: BinanceApi by lazy {
        createRetrofitInstance(host).create(BinanceApi::class.java)
    }
    private val client: BinanceDexApiRestClient by lazy {
        BinanceDexApiClientFactory.newInstance().newRestClient(
            if (!isTestNet) {
                BinanceDexEnvironment.PROD.baseUrl
            } else {
                BinanceDexEnvironment.TEST_NET.baseUrl
            },
        )
    }

    override suspend fun getInfo(address: String): Result<BinanceInfoResponse> {
        return try {
            val accountData = retryIO { client.getAccount(address) }
            val balances = accountData.balances.map { it.symbol to it.free.toBigDecimal() }.toMap()

            Result.Success(
                BinanceInfoResponse(
                    balances = balances,
                    accountNumber = accountData.accountNumber.toLong(),
                    sequence = accountData.sequence,
                ),
            )
        } catch (exception: Exception) {
            if (exception.message == "account not found") {
                Result.Success(
                    BinanceInfoResponse(
                        balances = emptyMap(),
                        accountNumber = null,
                        sequence = null,
                    ),
                )
            } else {
                Result.Failure(exception.toBlockchainSdkError())
            }
        }
    }

    override suspend fun getFee(): Result<BigDecimal> {
        return try {
            val feeData = api.getFees()
            var fee: BigDecimal? = null
            for (binanceFee in feeData) {
                if (binanceFee.transactionFee != null) {
                    fee = binanceFee.transactionFee?.value?.toBigDecimal()
                        ?.movePointLeft(Blockchain.Binance.decimals())
                    break
                }
            }
            return Result.Success(fee ?: error("Invalid fee response"))
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult {
        return try {
            val requestBody = TransactionRequestAssemblerExtSign.createRequestBody(transaction)
            val response = retryIO { client.broadcastNoWallet(requestBody, true) }
            if (response.isNotEmpty() && response[0].isOk) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.CustomError("transaction failed"))
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }
}
