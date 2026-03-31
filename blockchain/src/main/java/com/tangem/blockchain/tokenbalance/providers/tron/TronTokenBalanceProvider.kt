package com.tangem.blockchain.tokenbalance.providers.tron

import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.tokenbalance.TokenBalanceProvider
import com.tangem.blockchain.tokenbalance.models.TokenBalance
import java.math.BigDecimal

internal class TronTokenBalanceProvider(
    private val networkService: TronNetworkService,
) : TokenBalanceProvider {

    override suspend fun getTokenBalances(walletAddress: String): List<TokenBalance> {
        val accountResponse = when (val result = networkService.getV1Accounts(walletAddress)) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (accountResponse.address == null) return emptyList()

        val result = mutableListOf<TokenBalance>()

        accountResponse.trc20?.forEach { tokenMap ->
            tokenMap.entries.firstOrNull()?.let { (contractAddress, balanceString) ->
                val balanceValue = balanceString.toBigDecimalOrNull() ?: return@let
                if (balanceValue.compareTo(BigDecimal.ZERO) == 0) return@let
                result.add(
                    TokenBalance(
                        contractAddress = contractAddress,
                        amount = balanceValue,
                        isNativeToken = false,
                    ),
                )
            }
        }

        return result
    }
}