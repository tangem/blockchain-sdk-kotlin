package com.tangem.blockchain.blockchains.cardano.network.rosetta

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import com.tangem.blockchain.blockchains.cardano.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.RosettaNetwork
import com.tangem.blockchain.blockchains.cardano.network.rosetta.model.RosettaAccountIdentifier
import com.tangem.blockchain.blockchains.cardano.network.rosetta.model.RosettaAddressBody
import com.tangem.blockchain.blockchains.cardano.network.rosetta.model.RosettaNetworkIdentifier
import com.tangem.blockchain.blockchains.cardano.network.rosetta.model.RosettaSubmitBody
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.ByteArrayOutputStream

class RosettaNetworkProvider(rosettaNetwork: RosettaNetwork) : CardanoNetworkProvider {

    override val baseUrl: String = rosettaNetwork.url

    private val api: RosettaApi by lazy {
        createRetrofitInstance(rosettaNetwork.url).create(RosettaApi::class.java)
    }

    private val networkIdentifier = RosettaNetworkIdentifier("cardano", "mainnet")
//    private val nativeCurrency = RosettaCurrency("ADA", 6)

//    private var coinsMap: Map<String, List<RosettaCoin>> = emptyMap()
//    private var payloadsResponse: RosettaPayloadsResponse? = null

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        return try {
            coroutineScope {
                val addressBodies = addresses
                    .map { RosettaAddressBody(networkIdentifier, RosettaAccountIdentifier(it)) }
                val coinsDeferred = addressBodies.map {
                    it.accountIdentifier.address!! to retryIO { async { api.getCoins(it) } }
                }.toMap()

                val coinsMap = coinsDeferred.mapValues { it.value.await().coins!! }
                val unspentOutputs = coinsMap.flatMap { entry ->
                    entry.value.mapNotNull {
                        if (it.amount!!.currency!!.symbol == "ADA" &&
                            it.metadata == null // filter tokens while we don't support them
                        ) {
                            val identifierSplit =
                                it.coinIdentifier!!.identifier!!.split(":")
                            CardanoUnspentOutput(
                                address = entry.key,
                                amount = it.amount.value!!,
                                outputIndex = identifierSplit[1].toLong(),
                                transactionHash = identifierSplit[0].hexToBytes(),
                            )
                        } else {
                            null
                        }
                    }
                }
                val balance = unspentOutputs.sumOf { it.amount }

                Result.Success(
                    CardanoAddressResponse(balance, unspentOutputs, emptyList()),
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult {
        return try {
            val cborBuilder = CborBuilder().addArray().add(transaction.toHexString()).end()
            val baos = ByteArrayOutputStream()
            CborEncoder(baos).encode(cborBuilder.build())
            val encodedTransaction = baos.toByteArray()

            api.submitTransaction(
                RosettaSubmitBody(networkIdentifier, encodedTransaction.toHexString()),
            )
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

//    suspend fun prepareTransactionForSign(
//            transactionData: TransactionData
//    ): Result<RosettaPayloadsResponse> {
//        return try {
//            var index = 0
//            val operations = coinsMap.flatMap { entry -> // inputs
//                entry.value.map {
//                    RosettaOperation(
//                            operationIdentifier = RosettaOperationIdentifier(index++),
//                            type = "input",
//                            account = RosettaAccountIdentifier(entry.key),
//                            amount = RosettaAmount(-it.amount!!.value!!, it.amount.currency), // don't forget the minus
//                            coinChange = RosettaCoinChange("coin_spent", it.coinIdentifier),
//                            status = ""
//                    )
//                }
//            }.toMutableList()
//
//            operations.add(
//                    makeOutputOperation(
//                            index = index++,
//                            destinationAddress = transactionData.destinationAddress,
//                            amount = transactionData.amount.longValue!!
//                    )
//            )
//
//            val change = -operations.filter { it.amount!!.currency == nativeCurrency } // don't forget the minus
//                    .map { it.amount!!.value!! }.sum() - transactionData.fee!!.longValue!!
//
//            if (change > 0) {
//                operations.add(
//                        makeOutputOperation(
//                                index = index++,
//                                destinationAddress = transactionData.sourceAddress,
//                                amount = change
//                        )
//                )
//            }
//
//            val preprocessResponse = api.preprocessTransaction(
//                    RosettaPreprocessBody(
//                            networkIdentifier = networkIdentifier,
//                            operations = operations,
//                            metadata = RosettaRelativeTtlMetadata(20)
//                    )
//            )
//
//            val metadataResponse = api.getMetadata(
//                    RosettaMetadataBody(networkIdentifier, preprocessResponse.options!!)
//            )
//
//            payloadsResponse = api.getPayloads(
//                    RosettaPayloadsBody(networkIdentifier, operations, metadataResponse.metadata!!)
//            )
//
//            Result.Success(payloadsResponse!!)
//        } catch (exception: Exception) {
//            Result.Failure(exception)
//        }
//    }
//
//    suspend fun combineAndSendTransaction(signature: ByteArray, publicKey: ByteArray): SimpleResult {
//        val combineResponse = api.combineTransaction(
//                RosettaCombineBody(
//                        networkIdentifier = networkIdentifier,
//                        unsignedTransaction = payloadsResponse!!.unsignedTransaction!!,
//                        signatures = listOf(
//                                RosettaSignature(
//                                        signingPayload = payloadsResponse!!.payloads!![0],
//                                        publicKey = RosettaPublicKey(
//                                                hexBytes = publicKey.toHexString(),
//                                                curve_type = "edwards25519"
//                                        ),
//                                        signatureType = payloadsResponse!!.payloads!![0].signatureType,
//                                        hexBytes = signature.toHexString()
//                                )
//                        )
//                )
//        )
//
//        api.submitTransaction(
//                RosettaSubmitBody(networkIdentifier, combineResponse.signedTransaction!!)
//        )
//        return SimpleResult.Success
//    }
//
//    private fun makeOutputOperation(
//            index: Int,
//            destinationAddress: String,
//            amount: Long
//    ) = RosettaOperation(
//            operationIdentifier = RosettaOperationIdentifier(index),
//            type = "output",
//            account = RosettaAccountIdentifier(destinationAddress),
//            amount = RosettaAmount(
//                    value = amount,
//                    currency = nativeCurrency
//            ),
//            status = ""
//    )
}
