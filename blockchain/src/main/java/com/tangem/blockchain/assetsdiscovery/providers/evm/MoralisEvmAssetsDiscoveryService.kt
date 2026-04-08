package com.tangem.blockchain.assetsdiscovery.providers.evm

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.moralis.MoralisConstants
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.tokenbalance.providers.moralis.evm.network.MoralisEvmTokenBalanceApi
import com.tangem.blockchain.tokenbalance.providers.moralis.evm.network.MoralisEvmTokenBalanceItem
import java.math.BigDecimal

internal class MoralisEvmAssetsDiscoveryService(
    private val blockchain: Blockchain,
    apiKey: String?,
) : AssetsDiscoveryService {

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

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
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
            .mapNotNull { it.toBalance() }
            .filter { it.amount > BigDecimal.ZERO }
    }

    private fun Blockchain.toChainParam(): String =
        HEX_PREFIX + this.getChainId()?.let { Integer.toHexString(it) }.orEmpty()

    private fun MoralisEvmTokenBalanceItem.toBalance(): DiscoveredAsset? {
        val amount = parseAmount(
            rawBalance = balance,
            formattedBalance = balanceFormatted,
            decimals = decimals,
        ) ?: return null

        return if (isNativeToken) {
            DiscoveredAsset.Coin(symbol = blockchain.currency, amount = amount)
        } else {
            val address = tokenAddress ?: return null
            DiscoveredAsset.Token(contractAddress = address, amount = amount)
        }
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