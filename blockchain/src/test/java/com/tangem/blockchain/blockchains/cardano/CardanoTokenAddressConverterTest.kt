package com.tangem.blockchain.blockchains.cardano

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
class CardanoTokenAddressConverterTest(private val model: Model) {

    private val converter = CardanoTokenAddressConverter()

    @Test
    fun test() {
        val expected = converter.convertToFingerprint(model.address)

        Truth.assertThat(expected).isEqualTo(model.actual)
    }

    data class Model(
        private val policyId: String,
        private val assetName: String = "",
        val actual: String,
    ) {
        val address = policyId + assetName
    }

    private companion object {

        /**
         * Test cases taken from https://cips.cardano.org/cip/CIP-14
         */
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            Model(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                actual = "asset1rjklcrnsdzqp65wjgrg55sy9723kw09mlgvlc3",
            ),

            Model(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetName = "504154415445",
                actual = "asset13n25uv0yaf5kus35fm2k86cqy60z58d9xmde92",
            ),
            Model(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc37e",
                actual = "asset1nl0puwxmhas8fawxp8nx4e2q3wekg969n2auw3",
            ),
            Model(
                policyId = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                actual = "asset1uyuxku60yqe57nusqzjx38aan3f2wq6s93f6ea",
            ),
            Model(
                policyId = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                assetName = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                actual = "asset1aqrdypg669jgazruv5ah07nuyqe0wxjhe2el6f",
            ),
            Model(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetName = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                actual = "asset17jd78wukhtrnmjh3fngzasxm8rck0l2r4hhyyt",
            ),
            Model(
                policyId = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                assetName = "504154415445",
                actual = "asset1hv4p5tv2a837mzqrst04d0dcptdjmluqvdx9k3",
            ),
            Model(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetName = "0000000000000000000000000000000000000000000000000000000000000000",
                actual = "asset1pkpwyknlvul7az0xx8czhl60pyel45rpje4z8w",
            ),
            Model(
                policyId = "asset1pkpwyknlvul7az0xx8czhl60pyel45rpje4z8w",
                actual = "asset1pkpwyknlvul7az0xx8czhl60pyel45rpje4z8w",
            ),
        )
    }
}