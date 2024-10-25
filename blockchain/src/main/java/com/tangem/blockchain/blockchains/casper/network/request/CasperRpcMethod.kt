package com.tangem.blockchain.blockchains.casper.network.request

/**
 * Casper rpc method
 *
 * @property name method name
 */
internal sealed class CasperRpcMethod(val name: String) {

    data object QueryBalance : CasperRpcMethod(name = "query_balance")

    data object AccountPutDeploy : CasperRpcMethod(name = "account_put_deploy")
}