package com.tangem.blockchain.blockchains.cardano.network.rosetta

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.InfoInput
import com.tangem.blockchain.blockchains.cardano.network.common.converters.CardanoAddressResponseConverter
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.rosetta.converters.RosettaUnspentOutputsConverter
import com.tangem.blockchain.blockchains.cardano.network.rosetta.request.RosettaCoinsBody
import com.tangem.blockchain.blockchains.cardano.network.rosetta.request.RosettaNetworkIdentifier
import com.tangem.blockchain.blockchains.cardano.network.rosetta.request.RosettaSubmitBody
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.ByteArrayOutputStream

internal class RosettaNetworkProvider(
    rosettaNetwork: RosettaNetwork,
    override val baseUrl: String = rosettaNetwork.url,
) : CardanoNetworkProvider {

    private val api: RosettaApi by lazy {
        createRetrofitInstance(baseUrl).create(RosettaApi::class.java)
    }

    override suspend fun getInfo(input: InfoInput): Result<CardanoAddressResponse> {
        return try {
            coroutineScope {
                val addressBodies = input.addresses.map {
                    RosettaCoinsBody(
                        networkIdentifier = NETWORK_IDENTIFIER,
                        accountIdentifier = RosettaCoinsBody.AccountIdentifier(it),
                    )
                }

                val coinsMap = addressBodies
                    .associate { addressBody ->
                        addressBody.accountIdentifier.address to async { api.getCoins(addressBody) }
                    }
                    .mapValues { it.value.await().coins }

                val unspentOutputs = RosettaUnspentOutputsConverter.convert(coinsMap)

                Result.Success(
                    CardanoAddressResponseConverter.convert(
                        unspentOutputs = unspentOutputs,
                        tokens = input.tokens,
                        recentTransactionsHashes = emptyList(),
                    ),
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
                RosettaSubmitBody(NETWORK_IDENTIFIER, encodedTransaction.toHexString()),
            )
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    private companion object {
        val NETWORK_IDENTIFIER = RosettaNetworkIdentifier(blockchain = "cardano", network = "mainnet")
    }
}