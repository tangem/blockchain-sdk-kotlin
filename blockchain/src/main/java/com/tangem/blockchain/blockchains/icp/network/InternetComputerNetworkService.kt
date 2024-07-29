package com.tangem.blockchain.blockchains.icp.network

import com.tangem.blockchain.network.MultiNetworkProvider
import org.ic4j.agent.TangemUtils
import org.ic4j.agent.icp.IcpMethod
import org.ic4j.agent.identity.AnonymousIdentity

internal class InternetComputerNetworkService(networkProviders: List<InternetComputerNetworkProvider>) {

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl
    private val multiJsonRpcProvider = MultiNetworkProvider(networkProviders)
    private val anonymousIdentity = AnonymousIdentity()

    suspend fun getBalance(address: String) {
        val cborEncodedPayload = TangemUtils.constructEnvelopePayload(
            /* method */ IcpMethod.balance(address.toByteArray().toTypedArray()),
            /* identity */ anonymousIdentity,
        )
        multiJsonRpcProvider.performRequest { getBalance(cborEncodedPayload) }
    }
}