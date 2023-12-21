package com.tangem.blockchain.blockchains.telos

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class TelosWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider) {

    override suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        data: String?,
    ): Result<TransactionFee> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    if (data != null) {
                        getGasLimit(amount, destination, data)
                    } else {
                        getGasLimit(amount, destination)
                    }
                }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = gasLimitResponsesDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }
                val gPrice = gasPriceResponsesDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }

                val fee = feesCalculator.calculateSingleFee(
                    amountParams = getAmountParams(),
                    gasLimit = gLimit,
                    gasPrice = gPrice
                )

                Result.Success(fee)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }
}