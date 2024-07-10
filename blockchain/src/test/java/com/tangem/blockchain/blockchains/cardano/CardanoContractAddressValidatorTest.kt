package com.tangem.blockchain.blockchains.cardano

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class CardanoContractAddressValidatorTest(private val model: Model) {

    private val service = CardanoAddressServiceFacade()

    @Test
    fun test() {
        val expected = service.validateContractAddress(model.address)

        Truth.assertThat(expected).isEqualTo(model.actual)
    }

    data class Model(val address: String, val actual: Boolean)

    private companion object {

        /**
         * Test cases taken from https://cips.cardano.org/cip/CIP-14
         */
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            // WMT, AssetID
            Model(
                address = "1d7f33bd23d85e1a25d87d86fac4f199c3197a2f7afeb662a0f34e1e776f726c646d6f62696c65746f6b656e",
                actual = true,
            ),
            // AGIX, PolicyID
            Model(address = "f43a62fdc3965df486de8a0d32fe800963589c41b38946602a0dc535", actual = true),
            // Fingerprint
            Model(address = "asset1rjklcrnsdzqp65wjgrg55sy9723kw09mlgvlc3", actual = true),
            // Invalid
            Model(address = "f43a62fdc3965df486de8a0d32fe800963589c41b38946602a0dc53", actual = false),
        )
    }
}