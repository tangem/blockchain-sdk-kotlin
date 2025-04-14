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
        val expected = when (model) {
            is Model.PolicyID -> converter.convertToFingerprint(address = model.policyId, symbol = model.assetName)
            is Model.AssetID -> converter.convertToFingerprint(address = model.policyId + model.assetNameHex)
            is Model.Fingerprint -> converter.convertToFingerprint(address = model.value)
        }

        Truth.assertThat(expected).isEqualTo(model.actual)
    }

    sealed interface Model {

        val actual: String?

        data class PolicyID(val policyId: String, val assetName: String?, override val actual: String?) : Model

        data class AssetID(val policyId: String, val assetNameHex: String, override val actual: String?) : Model

        data class Fingerprint(val value: String, override val actual: String?) : Model
    }

    private companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            Model.PolicyID(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetName = null,
                actual = "asset1rjklcrnsdzqp65wjgrg55sy9723kw09mlgvlc3",
            ),
            Model.AssetID(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetNameHex = "504154415445",
                actual = "asset13n25uv0yaf5kus35fm2k86cqy60z58d9xmde92",
            ),
            Model.PolicyID(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc37e",
                assetName = null,
                actual = "asset1nl0puwxmhas8fawxp8nx4e2q3wekg969n2auw3",
            ),
            Model.PolicyID(
                policyId = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                assetName = null,
                actual = "asset1uyuxku60yqe57nusqzjx38aan3f2wq6s93f6ea",
            ),
            Model.AssetID(
                policyId = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                assetNameHex = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                actual = "asset1aqrdypg669jgazruv5ah07nuyqe0wxjhe2el6f",
            ),
            Model.AssetID(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetNameHex = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                actual = "asset17jd78wukhtrnmjh3fngzasxm8rck0l2r4hhyyt",
            ),
            Model.AssetID(
                policyId = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df209",
                assetNameHex = "504154415445",
                actual = "asset1hv4p5tv2a837mzqrst04d0dcptdjmluqvdx9k3",
            ),
            Model.AssetID(
                policyId = "7eae28af2208be856f7a119668ae52a49b73725e326dc16579dcc373",
                assetNameHex = "0000000000000000000000000000000000000000000000000000000000000000",
                actual = "asset1pkpwyknlvul7az0xx8czhl60pyel45rpje4z8w",
            ),
            Model.Fingerprint(
                value = "asset1pkpwyknlvul7az0xx8czhl60pyel45rpje4z8w",
                actual = "asset1pkpwyknlvul7az0xx8czhl60pyel45rpje4z8w",
            ),
            Model.PolicyID(
                policyId = "8fef2d34078659493ce161a6c7fba4b56afefa8535296a5743f69587",
                assetName = "AADA",
                actual = "asset1khk46tdfsknze9k84ae0ee0k2x8mcwhz93k70d",
            ),
            Model.Fingerprint(
                value = "4.123)(=-",
                actual = null,
            ),
            Model.Fingerprint(
                value = "1e349c9bdea19fd6c147626a5260bc44b71635f398b67c59881df20p", // Invalid hex
                actual = null,
            ),
        )
    }
}