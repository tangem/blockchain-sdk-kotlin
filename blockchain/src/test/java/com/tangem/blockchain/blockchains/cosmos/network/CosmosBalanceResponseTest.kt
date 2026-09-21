package com.tangem.blockchain.blockchains.cosmos.network

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.network.moshi
import org.junit.Test
import java.math.BigDecimal

class CosmosBalanceResponseTest {

    private val adapter = moshi.adapter(CosmosBalanceResponse::class.java)

    private fun balances(json: String): List<CosmosBalance> {
        return requireNotNull(adapter.fromJson(json)?.balances)
    }

    @Test
    fun `alien denom holding more than Long MAX_VALUE does not break the response`() {
        val actual = balances(RESPONSE_WITH_ALIEN_IBC_DENOM)

        assertThat(actual).containsExactly(
            CosmosBalance(denom = ALIEN_DENOM, amount = BigDecimal("345000000000000000000")),
            CosmosBalance(denom = "uatom", amount = BigDecimal("1257887")),
        ).inOrder()
    }

    @Test
    fun `native denom is scaled without losing precision next to an alien denom`() {
        val uatom = balances(RESPONSE_WITH_ALIEN_IBC_DENOM).first { it.denom == "uatom" }

        assertThat(uatom.amount.movePointLeft(COSMOS_DECIMALS)).isEqualToIgnoringScale("1.257887")
    }

    @Test
    fun `amount of Long MAX_VALUE is parsed`() {
        val actual = balances(response(amount = "9223372036854775807"))

        assertThat(actual.single().amount).isEqualTo(BigDecimal("9223372036854775807"))
    }

    @Test
    fun `amount just above Long MAX_VALUE is parsed`() {
        val actual = balances(response(amount = "9223372036854775808"))

        assertThat(actual.single().amount).isEqualTo(BigDecimal("9223372036854775808"))
    }

    @Test
    fun `amount given as a JSON number is parsed`() {
        val actual = balances("""{ "balances": [ { "denom": "uatom", "amount": 1257887 } ] }""")

        assertThat(actual.single().amount).isEqualToIgnoringScale("1257887")
    }

    @Test
    fun `empty balances list is parsed`() {
        assertThat(balances("""{ "balances": [] }""")).isEmpty()
    }

    private companion object {

        const val COSMOS_DECIMALS = 6
        const val ALIEN_DENOM = "ibc/D0BD765CF2EC6B97264795351BD75685A7B806F857D7D84633F5AC5E4A9812ED"

        /**
         * Trimmed response of the address from SE-181: an unsolicited IBC token with 18 decimals sits
         * next to uatom and carries 3.45e20, which does not fit into Long.
         */
        val RESPONSE_WITH_ALIEN_IBC_DENOM = """
            {
              "balances": [
                { "denom": "$ALIEN_DENOM", "amount": "345000000000000000000" },
                { "denom": "uatom", "amount": "1257887" }
              ],
              "pagination": { "next_key": null, "total": "20" }
            }
        """.trimIndent()

        fun response(amount: String): String = """
            {
              "balances": [
                { "denom": "uatom", "amount": "$amount" }
              ],
              "pagination": { "next_key": null, "total": "1" }
            }
        """.trimIndent()
    }
}