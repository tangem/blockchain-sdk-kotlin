package com.tangem.blockchain.blockchains.ton.network

import com.tangem.blockchain.blockchains.ton.models.JettonData
import com.tangem.blockchain.blockchains.ton.models.TonWalletInfo
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.RoundingMode

internal class TonNetworkService(
    jsonRpcProviders: List<TonNetworkProvider>,
    private val blockchain: Blockchain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(jsonRpcProviders)

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getWalletInformation(address: String, tokens: Set<Token>): Result<TonWalletInfo> {
        return try {
            coroutineScope {
                val jettonWalletAddressesDeferred = tokens.map { token ->
                    token to async {
                        multiJsonRpcProvider.performRequest(
                            TonNetworkProvider::getJettonWalletAddress,
                            GetJettonWalletAddressInput(address, token.contractAddress),
                        )
                    }
                }.toMap()
                val walletInformationDeferred = async {
                    multiJsonRpcProvider.performRequest(TonNetworkProvider::getWalletInformation, address)
                }

                val jettonWalletAddresses = jettonWalletAddressesDeferred.mapValues { entry ->
                    entry.value.await().successOr { return@coroutineScope it }
                }
                val jettonBalancesDeffered = jettonWalletAddresses.mapValues { entry ->
                    async {
                        multiJsonRpcProvider.performRequest(
                            TonNetworkProvider::getJettonBalance,
                            entry.value,
                        )
                    }
                }

                val walletInformation = walletInformationDeferred.await().successOr { return@coroutineScope it }
                val jettonBalances = jettonBalancesDeffered.mapValues { entry ->
                    entry.value.await().successOr { return@coroutineScope it }
                }

                Result.Success(
                    TonWalletInfo(
                        balance = walletInformation.balance.movePointLeft(blockchain.decimals()),
                        sequenceNumber = walletInformation.seqno ?: 0,
                        jettonDatas = jettonWalletAddresses.mapValues { entry ->
                            JettonData(
                                jettonBalances[entry.key]!!.toBigDecimal().movePointLeft(entry.key.decimals),
                                entry.value,
                            )
                        }
                    ),
                )
            }
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
            val hashResponse = multiJsonRpcProvider.performRequest(TonNetworkProvider::send, message)
                .successOr { return it }
            Result.Success(hashResponse.hash)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}