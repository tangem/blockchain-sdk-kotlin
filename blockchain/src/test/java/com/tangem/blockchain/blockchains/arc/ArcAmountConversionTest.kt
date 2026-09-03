package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.onChainDecimals
import com.tangem.blockchain.blockchains.ethereum.toOnChainValue
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for converting amounts into the value Arc operates with: the USDC coin has 6 decimals, while the chain
 * expects 18 decimals in a transaction.
 */
internal class ArcAmountConversionTest {

    @Test
    fun `Arc coin amount is converted to 18 decimals`() {
        val amount = Amount(value = BigDecimal("1.5"), blockchain = Blockchain.Arc)

        Truth.assertThat(amount.decimals).isEqualTo(6)
        Truth.assertThat(amount.onChainDecimals(Blockchain.Arc)).isEqualTo(18)
        Truth.assertThat(amount.toOnChainValue(Blockchain.Arc)).isEqualTo(BigInteger("1500000000000000000"))
    }

    @Test
    fun `Arc token amount is converted with its contract decimals`() {
        val token = Token(name = "Tether", symbol = "USDT", contractAddress = CONTRACT_ADDRESS, decimals = 6)
        val amount = Amount(token = token, value = BigDecimal("1.5"))

        Truth.assertThat(amount.onChainDecimals(Blockchain.Arc)).isEqualTo(6)
        Truth.assertThat(amount.toOnChainValue(Blockchain.Arc)).isEqualTo(BigInteger("1500000"))
    }

    @Test
    fun `Ethereum coin amount conversion is unchanged`() {
        val amount = Amount(value = BigDecimal("1.5"), blockchain = Blockchain.Ethereum)

        Truth.assertThat(amount.onChainDecimals(Blockchain.Ethereum)).isEqualTo(18)
        Truth.assertThat(amount.toOnChainValue(Blockchain.Ethereum)).isEqualTo(BigInteger("1500000000000000000"))
    }

    @Test
    fun `amount without value is not converted`() {
        val amount = Amount(value = null, blockchain = Blockchain.Arc)

        Truth.assertThat(amount.toOnChainValue(Blockchain.Arc)).isNull()
    }

    private companion object {
        const val CONTRACT_ADDRESS = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
}