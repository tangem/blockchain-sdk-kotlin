package com.tangem.blockchain.blockchains.ton.network

import com.tangem.blockchain.blockchains.ton.TonWalletInfo
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.RoundingMode

class TonNetworkService(
    jsonRpcProviders: List<TonJsonRpcNetworkProvider>,
    private val blockchain: Blockchain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(jsonRpcProviders)

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getWalletInformation(address: String): Result<TonWalletInfo> {
        return try {
            val addressInformation = multiJsonRpcProvider
                .performRequest(TonJsonRpcNetworkProvider::getWalletInformation, address)
                .successOr { return it }
            Result.Success(
                TonWalletInfo(
                    balance = addressInformation.balance.movePointLeft(blockchain.decimals()),
                    sequenceNumber = addressInformation.seqno ?: 0,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getFee(address: String, message: String): Result<Amount> {
        return try {
            val feeResponse = multiJsonRpcProvider.performRequest { getFee(address, message) }
                .successOr { return Result.Failure(BlockchainSdkError.FailedToLoadFee) }
            val totalFee = feeResponse.sourceFees.totalFee.movePointLeft(blockchain.decimals())
            Result.Success(
                Amount(
                    value = totalFee.setScale(2, RoundingMode.UP),
                    blockchain = blockchain,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun send(message: String): Result<String> {
        return try {
            val hashResponse = multiJsonRpcProvider.performRequest(TonJsonRpcNetworkProvider::send, message)
                .successOr { return it }
            Result.Success(hashResponse.hash)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}
