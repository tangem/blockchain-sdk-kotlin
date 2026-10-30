package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

/**
 * [coins] and [suiCoinsBalance] cover only `Coin<T>` objects, the inputs WalletCore can spend. [mistAddressBalances]
 * are the funds held in the address balance (SIP-58) per coin type.
 */
internal data class SuiWalletInfo(
    val suiCoinsBalance: BigDecimal,
    val coins: List<SuiCoin>,
    val mistAddressBalances: Map<String, BigDecimal>,
) {

    fun coinsOf(coinType: String): List<SuiCoin> = coins.filter { it.coinType == coinType }

    fun mistAddressBalanceOf(coinType: String): BigDecimal = mistAddressBalances[coinType] ?: BigDecimal.ZERO

    /**
     * Summed up from the same coins and address balances the transactions are built from, so that sending the
     * whole balance matches them even if the balance changed between the requests
     */
    fun mistBalanceOf(coinType: String): BigDecimal {
        return coinsOf(coinType).sumOf { it.mistBalance } + mistAddressBalanceOf(coinType)
    }
}