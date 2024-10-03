package com.tangem.blockchain.blockchains.filecoin.network.request

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.converters.FilecoinTransactionBodyConverter
import com.tangem.blockchain.common.JsonRPCRequest

/**
 * Factory for creating [JsonRPCRequest]
 *
 * @see <a href="https://github.com/filecoin-project/lotus/tree/master/node/impl/full">Filecoin JSON-RPC
 * implementations</a>
 *
[REDACTED_AUTHOR]
 */
internal object FilecoinRpcBodyFactory {

    /**
     * Create get actor info body
     *
     * @param address address
     */
    fun createGetActorInfoBody(address: String) = create(
        method = FilecoinRpcMethod.GetActorInfo,
        params = listOf<Any?>(address, null),
    )

    /**
     * Create get message gas body
     *
     * @param transactionInfo transaction info
     */
    fun createGetMessageGasBody(transactionInfo: FilecoinTxInfo) = create(
        method = FilecoinRpcMethod.GetMessageGas,
        params = listOf<Any?>(
            FilecoinTransactionBodyConverter.convert(from = transactionInfo),
            null,
            null,
        ),
    )

    /**
     * Create submit transaction body
     *
     * @param signedTransactionBody signed transaction body
     */
    fun createSubmitTransactionBody(signedTransactionBody: FilecoinSignedTransactionBody) = create(
        method = FilecoinRpcMethod.SubmitTransaction,
        params = listOf<Any?>(signedTransactionBody),
    )

    private fun create(method: FilecoinRpcMethod, params: List<Any?>): JsonRPCRequest {
        return JsonRPCRequest(
            id = "1",
            method = method.name,
            params = params,
        )
    }
}