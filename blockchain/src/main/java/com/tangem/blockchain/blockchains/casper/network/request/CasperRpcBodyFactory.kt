package com.tangem.blockchain.blockchains.casper.network.request

import com.tangem.blockchain.blockchains.casper.network.converters.CasperBalanceBodyConverter
import com.tangem.blockchain.common.JsonRPCRequest

/**
 * Factory for creating [JsonRPCRequest]
 *
 * @see <a href="https://github.com/casper-network/casper-sidecar/blob/feat-2.0/resources/test/rpc_schema.json">Casper JSON-RPC schema</a>
 *
 */
internal object CasperRpcBodyFactory {

    /**
     * Create query balance body
     *
     * @param address address
     */
    fun createQueryBalanceBody(address: String) = create(
        method = CasperRpcMethod.QueryBalance,
        params = CasperBalanceBodyConverter.convert(address),
    )

    private fun create(method: CasperRpcMethod, params: Any?): JsonRPCRequest {
        return JsonRPCRequest(
            id = "1",
            method = method.name,
            params = params,
        )
    }
}