package com.tangem.blockchain.blockchains.telos

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TelosWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
) : EthereumWalletManager(
    wallet = wallet,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    supportsENS = false,
) {

    override suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
        isSimulate: Boolean,
        spenderAddress: String?,
    ): Result<TransactionFee> {
        return if (wallet.blockchain.isSupportEIP1559) {
            getEIP1559Fee(
                amount = amount,
                destination = destination,
                callData = callData,
                spenderAddress = spenderAddress,
                isSimulate = isSimulate,
            )
        } else {
            getLegacyFee(
                amount = amount,
                destination = destination,
                callData = callData,
                spenderAddress = spenderAddress,
                isSimulate = isSimulate,
            )
        }
    }

    private suspend fun getEIP1559Fee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
        spenderAddress: String?,
        isSimulate: Boolean,
    ): Result<TransactionFee> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    val isSimulateWithOverride = isSimulate && spenderAddress != null
                    if (isSimulateWithOverride && callData != null && amount.type is AmountType.Token) {
                        Logger.logNetwork("ETH: start gas estimate")
                        getGasLimitWithOverride(
                            destination = destination,
                            spenderAddress = spenderAddress,
                            amountType = amount.type,
                            callData = callData,
                        )
                    } else if (callData != null) {
                        getGasLimit(amount, destination, callData)
                    } else {
                        getGasLimit(amount, destination)
                    }
                }

                val feeHistoryResponseDeferred = async { networkProvider.getFeeHistory() }

                val gLimit = gasLimitResponsesDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }

                val feeHistory = feeHistoryResponseDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }

                val fees = feesCalculator.calculateEip1559SingleFee(
                    amountParams = getAmountParams(),
                    gasLimit = gLimit,
                    feeHistory = feeHistory,
                )

                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun getLegacyFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
        spenderAddress: String?,
        isSimulate: Boolean,
    ): Result<TransactionFee> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    val isSimulateWithOverride = isSimulate && spenderAddress != null
                    if (isSimulateWithOverride && callData != null && amount.type is AmountType.Token) {
                        Logger.logNetwork("ETH: start gas estimate")
                        getGasLimitWithOverride(
                            destination = destination,
                            spenderAddress = spenderAddress,
                            amountType = amount.type,
                            callData = callData,
                        )
                    } else if (callData != null) {
                        getGasLimit(amount, destination, callData)
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
                    gasPrice = gPrice,
                )

                Result.Success(fee)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }
}