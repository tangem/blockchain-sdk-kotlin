package com.tangem.blockchain.extensions

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class StringKtTest(private val model: Model) {

    @Test
    fun isValidHex() {
        val actual = model.address.isValidHex()

        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val address: String, val expected: Boolean)

    private companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            Model(address = "", expected = true),
            Model(address = "0a", expected = true),
            Model(address = "0A1f", expected = true),
            Model(address = "f71b4cf652d8edb33a57928b8b8a546a3c954b7ba24db5583ac79b34", expected = true),
            // Odd length: cannot be decoded into bytes
            Model(address = "a", expected = false),
            Model(address = "f71b4cf652d8edb33a57928b8b8a546a3c954b7ba24db5583ac79b345", expected = false),
            // Non-hex characters
            Model(address = "0g", expected = false),
            Model(address = "zz", expected = false),
            Model(address = "asset1rjklcrnsdzqp65wjgrg55sy9723kw09mlgvlc3", expected = false),
            // Callers are expected to strip the prefix themselves
            Model(address = "0x1234", expected = false),
            Model(address = " 12", expected = false),
        )
    }
}