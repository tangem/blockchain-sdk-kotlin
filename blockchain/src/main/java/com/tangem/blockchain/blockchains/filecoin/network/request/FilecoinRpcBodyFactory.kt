package com.tangem.blockchain.blockchains.filecoin.network.request

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.converters.FilecoinTransactionBodyConverter

/**
 * Factory for creating [FilecoinRpcBody]
 *
 * @see <a href="https://github.com/filecoin-project/lotus/tree/master/node/impl/full">Filecoin JSON-RPC
 * implementations</a>
 *
 * @author Andrew Khokhlov on 30/07/2024
 */
internal object FilecoinRpcBodyFactory {

    /**
     * Create get actor info body
     *
     * @param address address
     */
    fun createGetActorInfoBody(address: String) = create(
        method = FilecoinRpcBody.Method.GetActorInfo,
        params = listOf<Any?>(address, null),
    )

    /**
     * Create get message gas body
     *
     * @param transactionInfo transaction info
     */
    fun createGetMessageGasBody(transactionInfo: FilecoinTxInfo) = create(
        method = FilecoinRpcBody.Method.GetMessageGas,
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
        method = FilecoinRpcBody.Method.SubmitTransaction,
        params = listOf<Any?>(signedTransactionBody),
    )

    private fun create(method: FilecoinRpcBody.Method, params: List<Any?>): FilecoinRpcBody {
        return FilecoinRpcBody(
            id = 1,
            jsonrpc = "2.0",
            method = method,
            params = params,
        )
    }
}
