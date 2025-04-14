package com.tangem.blockchain.blockchains.filecoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.converters.FilecoinTransactionBodyConverter
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinRpcBodyFactory
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinRpcMethod
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinSignedTransactionBody
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinTransactionBody
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.JsonRPCRequest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class FilecoinRpcBodyFactoryTest {

    @Test
    fun test_create_GetActorInfo_body() {
        val address = "address"

        val expected = JsonRPCRequest(
            id = "1",
            method = FilecoinRpcMethod.GetActorInfo.name,
            params = listOf<Any?>(address, null),
        )

        val actual = FilecoinRpcBodyFactory.createGetActorInfoBody(address)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_create_GetMessageGas_body() {
        val expected = JsonRPCRequest(
            id = "1",
            method = FilecoinRpcMethod.GetMessageGas.name,
            params = listOf<Any?>(
                FilecoinTransactionBodyConverter.convert(from = txInfo),
                null,
                null,
            ),
        )

        val actual = FilecoinRpcBodyFactory.createGetMessageGasBody(txInfo)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_create_SubmitTransaction_body() {
        val expected = JsonRPCRequest(
            id = "1",
            method = FilecoinRpcMethod.SubmitTransaction.name,
            params = listOf<Any?>(signedTransactionBody),
        )

        val actual = FilecoinRpcBodyFactory.createSubmitTransactionBody(signedTransactionBody)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private companion object {

        val txInfo = FilecoinTxInfo(
            sourceAddress = "inimicus",
            destinationAddress = "sadipscing",
            amount = 4135.toBigDecimal().movePointRight(Blockchain.Filecoin.decimals()).toBigInteger(),
            nonce = 7532,
        )

        val signedTransactionBody = FilecoinSignedTransactionBody(
            transactionBody = FilecoinTransactionBody(
                sourceAddress = "turpis",
                destinationAddress = "singulis",
                amount = "fabellas",
                nonce = 0,
                gasUnitPrice = "1000",
                gasLimit = 2000,
                gasPremium = "3000",
            ),
            signature = FilecoinSignedTransactionBody.Signature(
                type = 2739,
                signature = "tamquam",
            ),
        )
    }
}