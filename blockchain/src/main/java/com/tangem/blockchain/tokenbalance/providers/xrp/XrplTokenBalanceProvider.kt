package com.tangem.blockchain.tokenbalance.providers.xrp

import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTokenBalance
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.tokenbalance.TokenBalanceProvider
import com.tangem.blockchain.tokenbalance.models.TokenBalance
import java.math.BigDecimal

internal class XrplTokenBalanceProvider(
    private val networkProvider: XrpNetworkProvider,
) : TokenBalanceProvider {

    override suspend fun getTokenBalances(walletAddress: String): List<TokenBalance> {
        val infoResponse = when (val result = networkProvider.getInfo(walletAddress)) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (!infoResponse.accountFound) return emptyList()

        val result = mutableListOf<TokenBalance>()

        infoResponse.tokenBalances
            .filter { it.balance > BigDecimal.ZERO }
            .map { it.toTokenBalance() }
            .let { result.addAll(it) }

        return result
    }

    private fun XrpTokenBalance.toTokenBalance(): TokenBalance = TokenBalance(
        contractAddress = "$currency.$issuer",
        amount = balance,
        isNativeToken = false,
    )
}