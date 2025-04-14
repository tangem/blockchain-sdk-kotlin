package com.tangem.blockchain.blockchains.filecoin.network

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinAccountInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxGasInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinSignedTransactionBody
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider

/**
 * Filecoin network service
 *
 * @param providers list of [FilecoinNetworkProvider]
 *
[REDACTED_AUTHOR]
 */
internal class FilecoinNetworkService(providers: List<FilecoinNetworkProvider>) : FilecoinNetworkProvider {

    override val baseUrl: String
        get() = multiJsonRpcProvider.currentProvider.baseUrl

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    override suspend fun getAccountInfo(address: String): Result<FilecoinAccountInfo> {
        return try {
            val accountInfo = multiJsonRpcProvider.performRequest(FilecoinNetworkProvider::getAccountInfo, address)
                .successOr { return it }

            Result.Success(accountInfo)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun estimateMessageGas(transactionInfo: FilecoinTxInfo): Result<FilecoinTxGasInfo> {
        return try {
            val accountInfo = multiJsonRpcProvider.performRequest(
                request = FilecoinNetworkProvider::estimateMessageGas,
                data = transactionInfo,
            )
                .successOr { return it }

            Result.Success(accountInfo)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun submitTransaction(signedTransactionBody: FilecoinSignedTransactionBody): Result<String> {
        return try {
            val hash = multiJsonRpcProvider.performRequest(
                request = FilecoinNetworkProvider::submitTransaction,
                data = signedTransactionBody,
            )
                .successOr { return it }

            Result.Success(hash)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}