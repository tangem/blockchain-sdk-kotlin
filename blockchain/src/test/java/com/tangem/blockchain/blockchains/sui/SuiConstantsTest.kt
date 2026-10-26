package com.tangem.blockchain.blockchains.sui

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import org.junit.Test

class SuiConstantsTest {

    @Test
    fun `GIVEN padded native coin object type WHEN coinTypeOf THEN returns short SUI coin type`() {
        val objectType = "0x0000000000000000000000000000000000000000000000000000000000000002::coin::Coin<" +
            "0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI>"

        Truth.assertThat(SuiConstants.coinTypeOf(objectType)).isEqualTo(SuiConstants.COIN_TYPE)
    }

    @Test
    fun `GIVEN token coin object type WHEN coinTypeOf THEN returns token type as is`() {
        val tokenType = "0xdba34672e30cb065b1f93e3ab55318768fd6fef66c15942c9f7cb846e2f900e7::usdc::USDC"
        val objectType = "0x0000000000000000000000000000000000000000000000000000000000000002::coin::Coin<$tokenType>"

        Truth.assertThat(SuiConstants.coinTypeOf(objectType)).isEqualTo(tokenType)
    }

    @Test
    fun `GIVEN coin of generic type WHEN coinTypeOf THEN keeps nested type parameters`() {
        val lpType = "0xabc::pool::LP<0x2::sui::SUI, 0xdef::usdc::USDC>"
        val objectType = "0x0000000000000000000000000000000000000000000000000000000000000002::coin::Coin<$lpType>"

        Truth.assertThat(SuiConstants.coinTypeOf(objectType)).isEqualTo(lpType)
    }
}