package com.tangem.blockchain.blockchains.vechain.network

import com.tangem.blockchain.blockchains.vechain.VechainAccountInfo
import com.tangem.blockchain.blockchains.vechain.VechainBlockInfo
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider

internal class VechainNetworkService(
    networkProviders: List<VechainNetworkProvider>,
    private val blockchain: Blockchain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(networkProviders)
    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getAccountInfo(address: String, pendingTxIds: Set<String>): Result<VechainAccountInfo> {
        return multiJsonRpcProvider.performRequest {
            getAccountInfo(
                decimals = blockchain.decimals(),
                address = address,
                pendingTxIds = pendingTxIds,
            )
        }
    }

    suspend fun getLatestBlock(): Result<VechainBlockInfo> {
        return multiJsonRpcProvider.performRequest(VechainNetworkProvider::getLatestBlock)
    }

    suspend fun sendTransaction(rawData: ByteArray): Result<VechainCommitTransactionResponse> {
        return multiJsonRpcProvider.performRequest {
            sendTransaction(rawData)
        }
    }
}