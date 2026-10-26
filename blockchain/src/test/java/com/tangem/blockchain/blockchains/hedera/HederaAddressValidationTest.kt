package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class HederaAddressValidationTest(private val model: Model) {

    private val addressService = HederaAddressService(isTestnet = false)

    @Test
    fun validate() {
        Truth.assertThat(addressService.validate(model.address)).isEqualTo(model.isValid)
    }

    @Test
    fun parseAccountId() {
        val parsed = runCatching { HederaUtils.parseAccountId(model.address).toString() }.getOrNull()

        Truth.assertThat(parsed).isEqualTo(model.parsed)
    }

    data class Model(val address: String, val isValid: Boolean, val parsed: String?) {
        override fun toString(): String = address.map { if (it.code > 0x7E) "<U+%04X>".format(it.code) else it }
            .joinToString("")
    }

    private companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Model> = listOf(
            Model(address = "0.0.123456", isValid = true, parsed = "0.0.123456"),
            Model(address = "0.0.0", isValid = true, parsed = "0.0.0"),
            Model(address = "1.2.3", isValid = true, parsed = "1.2.3"),
            Model(address = "0.0.123456-tmxpl", isValid = true, parsed = "0.0.123456"),
            Model(address = "0.0.123456-abcde", isValid = false, parsed = "0.0.123456"),
            Model(
                address = "0x00000000000000000000000000000000000004e2",
                isValid = true,
                parsed = "0.0.00000000000000000000000000000000000004e2",
            ),
            Model(
                address = "0.0.b794f5ea0ba39494ce839613fffba74279579268",
                isValid = true,
                parsed = "0.0.b794f5ea0ba39494ce839613fffba74279579268",
            ),
            // The vendored hedera-sdk-java drops everything before the last well-formed id
            Model(address = "0.0.123456​0.0.999999", isValid = false, parsed = null),
            Model(address = "0.0.123456­0.0.999999", isValid = false, parsed = null),
            Model(address = "0.0.123456‌0.0.999999", isValid = false, parsed = null),
            Model(address = "0.0.123456⁠0.0.999999", isValid = false, parsed = null),
            Model(address = "0.0.123456 0.0.999999", isValid = false, parsed = null),
            Model(address = "abc0.0.999999", isValid = false, parsed = null),
            Model(address = "0.0.1234560.0.999999", isValid = false, parsed = null),
            Model(address = "abc0.0.b794f5ea0ba39494ce839613fffba74279579268", isValid = false, parsed = null),
            Model(address = " 0.0.123456", isValid = false, parsed = null),
            Model(address = "0.0.123456 ", isValid = false, parsed = null),
            Model(address = "0.0.01", isValid = false, parsed = null),
            Model(address = "0.0.١٢٣", isValid = false, parsed = null),
            Model(address = "", isValid = false, parsed = null),
        )
    }
}