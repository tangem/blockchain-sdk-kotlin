package com.tangem.blockchain.tokenbalance

import com.tangem.blockchain.tokenbalance.models.TokenBalance

interface TokenBalanceProvider {

    suspend fun getTokenBalances(walletAddress: String): List<TokenBalance>
}