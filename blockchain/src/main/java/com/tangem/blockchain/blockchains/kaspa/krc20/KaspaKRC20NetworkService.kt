package com.tangem.blockchain.blockchains.kaspa.krc20

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider

class KaspaKRC20NetworkService(providers: List<KaspaKRC20NetworkProvider>) : KaspaKRC20NetworkProvider {

    private val multiNetworkProvider = MultiNetworkProvider(providers, Blockchain.Kaspa)
    override val baseUrl: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    override suspend fun getBalances(
        address: String,
        tokens: List<Token>,
    ): Result<Map<Token, Result<KaspaKRC20InfoResponse>>> {
        return multiNetworkProvider.performRequest { getBalances(address, tokens) }
    }
}