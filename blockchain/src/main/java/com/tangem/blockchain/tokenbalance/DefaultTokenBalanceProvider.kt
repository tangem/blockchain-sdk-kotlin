package com.tangem.blockchain.tokenbalance

import com.tangem.blockchain.tokenbalance.models.TokenBalance

internal object DefaultTokenBalanceProvider : TokenBalanceProvider {

    override suspend fun getTokenBalances(walletAddress: String): List<TokenBalance> = emptyList()
}