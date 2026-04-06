package com.tangem.blockchain.tokenbalance.providers.moralis.evm

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.moralis.MoralisConstants
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.tokenbalance.TokenBalanceProvider
import com.tangem.blockchain.tokenbalance.models.TokenBalance
import com.tangem.blockchain.tokenbalance.providers.moralis.evm.network.MoralisEvmTokenBalanceApi
import com.tangem.blockchain.tokenbalance.providers.moralis.evm.network.MoralisEvmTokenBalanceItem
import java.math.BigDecimal

internal class MoralisEvmTokenBalanceProvider(
    private val blockchain: Blockchain,
    apiKey: String?,
) : TokenBalanceProvider {

    private val api: MoralisEvmTokenBalanceApi = createRetrofitInstance(
        baseUrl = MoralisConstants.DEEP_INDEX_API_URL,
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put(MoralisConstants.API_KEY_HEADER, it) }
                },
            ),
        ),
    ).create(MoralisEvmTokenBalanceApi::class.java)

    override suspend fun getTokenBalances(walletAddress: String): List<TokenBalance> {
        val accumulator = mutableListOf<MoralisEvmTokenBalanceItem>()
        var cursor: String? = null
        do {
            val response = api.getTokenBalances(
                address = walletAddress,
                chain = blockchain.toChainParam(),
                cursor = cursor,
            )
            accumulator.addAll(response.result)
            cursor = response.cursor
        } while (cursor != null)

        return accumulator
            .filter { !it.isPossibleSpam }
            .mapNotNull { it.toTokenBalance() }
    }

    private fun Blockchain.toChainParam(): String =
        HEX_PREFIX + this.getChainId()?.let { Integer.toHexString(it) }.orEmpty()

    private fun MoralisEvmTokenBalanceItem.toTokenBalance(): TokenBalance? {
        val amount = parseAmount(
            rawBalance = balance,
            formattedBalance = balanceFormatted,
            decimals = decimals,
        ) ?: return null

        return TokenBalance(
            contractAddress = if (isNativeToken) null else tokenAddress,
            symbol = symbol,
            name = name,
            decimals = decimals,
            amount = amount,
            isNativeToken = isNativeToken,
        )
    }

    private fun parseAmount(rawBalance: String, formattedBalance: String, decimals: Int): BigDecimal? {
        if (decimals < 0) return null

        val rawDecimal = rawBalance.toBigDecimalOrNull()
        if (rawDecimal != null) {
            return rawDecimal.movePointLeft(decimals).stripTrailingZeros()
        }

        return formattedBalance.toBigDecimalOrNull()
    }
}