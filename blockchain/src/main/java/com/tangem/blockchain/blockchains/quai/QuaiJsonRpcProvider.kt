package com.tangem.blockchain.blockchains.quai

import com.tangem.blockchain.blockchains.ethereum.network.EthereumLikeJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.QuaiMethod

/**
 * JSON RPC provider for Quai Network
 * Uses quai_ prefixed methods instead of eth_
 */
internal class QuaiJsonRpcProvider(
    baseUrl: String,
    postfixUrl: String = "",
    authToken: String? = null,
    nowNodesApiKey: String? = null,
) : EthereumLikeJsonRpcProvider(baseUrl, postfixUrl, authToken, nowNodesApiKey) {

    override fun getMethods() = QuaiMethod
}