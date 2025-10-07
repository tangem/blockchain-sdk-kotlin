package com.tangem.blockchain.blockchains.near.network

import com.tangem.blockchain.blockchains.near.network.api.ProtocolConfigResult
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider

/**
[REDACTED_AUTHOR]
 */
class NearNetworkService(blockchain: Blockchain, providers: List<NearNetworkProvider>) {

    val host: String get() = multiNetworkProvider.currentProvider.baseUrl
    private val multiNetworkProvider = MultiNetworkProvider(providers, blockchain)

    init {
        if (blockchain != Blockchain.Near && blockchain != Blockchain.NearTestnet) {
            error("The blockchain parameter should be Near or NearTestnet")
        }
    }

    suspend fun getAccount(address: String): Result<NearAccount> {
        return when (val result = multiNetworkProvider.performRequest(NearNetworkProvider::getAccount, address)) {
            is Result.Success -> {
                Result.Success(
                    NearAccount.Full(
                        near = NearAmount(Yocto(result.data.amount)),
                        blockHash = result.data.blockHash,
                        storageUsage = NearAmount(Yocto(result.data.storageUsage.toBigInteger())),
                    ),
                )
            }

            is Result.Failure -> {
                val nearError = result.mapToNearError() ?: return result

                return if (nearError is NearError.UnknownAccount) {
                    Result.Success(NearAccount.NotInitialized)
                } else {
                    result
                }
            }
        }
    }

    suspend fun getAccessKey(address: String, publicKeyEncodedToBase58: String): Result<AccessKey> {
        val accessKeyResult =
            multiNetworkProvider.performRequest(
                request = NearNetworkProvider::getAccessKey,
                data = NearGetAccessKeyParams(address, publicKeyEncodedToBase58),
            ).successOr { return it }

        val accessKey = AccessKey(accessKeyResult.nonce, accessKeyResult.blockHeight, accessKeyResult.blockHash)
        return Result.Success(accessKey)
    }

    suspend fun getGas(blockHash: String?): Result<NearGasPrice> {
        val safeBlockHash = blockHash ?: getNetworkStatus()
            .successOr { return it }.latestBlockHash
        val gasPriceResult = multiNetworkProvider.performRequest(NearNetworkProvider::getGas, safeBlockHash)
            .successOr { return it }

        val gasPrice = NearGasPrice(
            yoctoGasPrice = Yocto(gasPriceResult.gasPrice),
            blockHash = safeBlockHash,
        )

        return Result.Success(gasPrice)
    }

    suspend fun sendTransaction(signedTxBase64: String): Result<String> {
        val sendTxHash = multiNetworkProvider.performRequest(NearNetworkProvider::sendTransaction, signedTxBase64)
            .successOr { return it }

        return Result.Success(sendTxHash)
    }

    suspend fun getStatus(txHash: String, senderId: String): Result<NearSentTransaction> {
        val sendTxResult = multiNetworkProvider.performRequest(
            request = NearNetworkProvider::getTransactionStatus,
            data = NearGetTxParams(txHash, senderId),
        ).successOr { return it }

        val nearWalletInfo = NearSentTransaction(
            hash = sendTxResult.transaction.hash,
            isSuccessful = sendTxResult.status.successValue != null,
        )

        return Result.Success(nearWalletInfo)
    }

    suspend fun getNetworkStatus(): Result<NearNetworkStatus> {
        val statusResult = multiNetworkProvider.performRequest(NearNetworkProvider::getNetworkStatus)
            .successOr { return it }

        val status = NearNetworkStatus(
            chainId = statusResult.chainId,
            latestBlockHash = statusResult.syncInfo.latestBlockHash,
            latestBlockHeight = statusResult.syncInfo.latestBlockHeight,
            latestBlockTime = statusResult.syncInfo.latestBlockTime,
            syncing = statusResult.syncInfo.syncing,
        )

        return Result.Success(status)
    }

    suspend fun getProtocolConfig(): Result<ProtocolConfigResult> {
        return multiNetworkProvider.performRequest(NearNetworkProvider::getProtocolConfig)
    }

    private fun Result.Failure.mapToNearError(): NearError? {
        return (this.error as? BlockchainSdkError.NearException.Api)?.let { NearError.mapFrom(it) }
    }
}