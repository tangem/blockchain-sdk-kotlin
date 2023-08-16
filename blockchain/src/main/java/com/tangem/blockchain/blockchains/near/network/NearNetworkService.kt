package com.tangem.blockchain.blockchains.near.network

import com.tangem.blockchain.blockchains.near.NearTransactionBuilder
import com.tangem.blockchain.blockchains.near.network.api.SendTransactionAsyncResult
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class NearNetworkService(
    private val blockchain: Blockchain,
    private val multiJsonRpcProvider: MultiNetworkProvider<NearJsonRpcNetworkProvider>,
    private val txBuilder: NearTransactionBuilder,
) {

    init {
        if (blockchain != Blockchain.Near && blockchain != Blockchain.NearTestnet) {
            throw IllegalArgumentException("The blockchain parameter should be Near or NearTestnet")
        }
    }

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getAccount(address: String): Result<NearAccount> {
        val result = multiJsonRpcProvider.performRequest(NearNetworkProvider::getAccount, address)

        return when (result) {
            is Result.Success -> {
                Result.Success(NearAccount.Full(NearAmount(Yocto(result.data.amount))))
            }

            is Result.Failure -> {
                val nearError = result.mapToNearError() ?: return result

                return if (nearError is NearError.UnknownAccount) {
                    Result.Success(NearAccount.Empty)
                } else {
                    result
                }
            }
        }
    }

    suspend fun getAccessKey(address: String): Result<AccessKey> {
        val accessKeyResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::getAccessKey, address)
            .successOr { return it }

        val accessKey = AccessKey(accessKeyResult.nonce, accessKeyResult.blockHeight, accessKeyResult.blockHash)
        return Result.Success(accessKey)
    }

    private suspend fun createAccount(address: String, amount: BigDecimal): Result<SendTransactionAsyncResult> {
        val nonce = 1L // if account doesn't exist, then nonce must be 1
        val status = getNetworkStatus()
            .successOr { return it }
        val builtTx = txBuilder.buildForCreateAccount(address, amount, nonce, status.latestBlockHash)
            .successOr { return it }
        val sentTxHash = multiJsonRpcProvider.performRequest(NearNetworkProvider::sendTransaction, builtTx)
            .successOr { return it }

        return Result.Success(sentTxHash)
    }

    suspend fun getGas(blockHash: String?): Result<NearGasPrice> {
        val safeBlockHash = blockHash ?: getNetworkStatus()
            .successOr { return it }.latestBlockHash
        val gasPriceResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::getGas, safeBlockHash)
            .successOr { return it }

        val gasPrice = NearGasPrice(
            yoctoGasPrice = Yocto(gasPriceResult.gasPrice),
            blockHash = safeBlockHash,
        )

        return Result.Success(gasPrice)
    }

    suspend fun sendTransaction(signedTxBase64: String): Result<NearSentTransaction> {
        val sendTxResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::sendTransaction, signedTxBase64)
            .successOr { return it }

        val nearWalletInfo = NearSentTransaction(
            hash = sendTxResult
        )

        return Result.Success(nearWalletInfo)
    }

    suspend fun getNetworkStatus(): Result<NearNetworkStatus> {
        val statusResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::getNetworkStatus)
            .successOr { return it }

        val status = NearNetworkStatus(
            chainId = statusResult.chainId,
            version = statusResult.version.version,
            latestBlockHash = statusResult.syncInfo.latestBlockHash,
            latestBlockHeight = statusResult.syncInfo.latestBlockHeight,
            latestBlockTime = statusResult.syncInfo.latestBlockTime,
            syncing = statusResult.syncInfo.syncing,
        )

        return Result.Success(status)
    }

    private fun Result.Failure.mapToNearError(): NearError? {
        return (this.error as? BlockchainSdkError.NearException.Api)?.let { NearError.mapFrom(it) }
    }
}