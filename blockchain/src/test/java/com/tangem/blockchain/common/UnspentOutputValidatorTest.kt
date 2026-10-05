package com.tangem.blockchain.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class UnspentOutputValidatorTest {

    @Test
    fun `index within uint32 range and positive amount is valid`() {
        assertThat(UnspentOutputValidator.isValid(outputIndex = 0, amount = BigDecimal.ONE)).isTrue()
        assertThat(UnspentOutputValidator.isValid(outputIndex = 4294967295L, amount = BigDecimal.ONE)).isTrue()
    }

    @Test
    fun `negative index is rejected`() {
        assertThat(UnspentOutputValidator.isValid(outputIndex = -1, amount = BigDecimal.ONE)).isFalse()
    }

    @Test
    fun `index above uint32 max is rejected`() {
        assertThat(UnspentOutputValidator.isValid(outputIndex = 4294967296L, amount = BigDecimal.ONE)).isFalse()
    }

    @Test
    fun `non-positive amount is rejected`() {
        assertThat(UnspentOutputValidator.isValid(outputIndex = 0, amount = BigDecimal.ZERO)).isFalse()
        assertThat(UnspentOutputValidator.isValid(outputIndex = 0, amount = BigDecimal("-0.5"))).isFalse()
    }
}