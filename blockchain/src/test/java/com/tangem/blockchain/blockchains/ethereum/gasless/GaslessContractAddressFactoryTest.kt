package com.tangem.blockchain.blockchains.ethereum.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

/**
 * Tests for [GaslessContractAddressFactory].
 *
 * Locks in the gasless executor (EIP-7702 delegation target) addresses per protocol version. These addresses are
 * hashed into the EIP-7702 authorization, so a wrong value silently breaks signing — they must never drift
 * unintentionally.
 */
internal class GaslessContractAddressFactoryTest {

    @Test
    fun `GIVEN Polygon WHEN v2 THEN returns the v2 executor address`() {
        val factory = GaslessContractAddressFactory(Blockchain.Polygon)

        assertThat(factory.getGaslessExecutorContractAddress(isV2 = true))
            .isEqualTo("0x5c5eB829353bdb38456B54480aB436cAE421B75C")
    }

    @Test
    fun `GIVEN Polygon WHEN v1 THEN returns the legacy executor address`() {
        val factory = GaslessContractAddressFactory(Blockchain.Polygon)

        assertThat(factory.getGaslessExecutorContractAddress(isV2 = false))
            .isEqualTo("0x2C2397c7605dc6d5493518260BDdeebE743B3faD")
    }

    @Test
    fun `GIVEN non-Polygon chains WHEN any version THEN address is version-agnostic`() {
        // Only Polygon has a distinct v2 deployment; every other supported chain shares one address.
        val sharedAddressChains = mapOf(
            Blockchain.Ethereum to "0xe3014E9AB2739aDeF234B3829C79128746160178",
            Blockchain.BSC to "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3",
            Blockchain.Base to "0x61dD8620410a2372CbE4946f9148671F38F93fC7",
            Blockchain.Arbitrum to "0x20e7016ff14Dd10f04028fE52aBBca34F44b6965",
        )

        sharedAddressChains.forEach { (blockchain, expected) ->
            val factory = GaslessContractAddressFactory(blockchain)
            assertThat(factory.getGaslessExecutorContractAddress(isV2 = true)).isEqualTo(expected)
            assertThat(factory.getGaslessExecutorContractAddress(isV2 = false)).isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN unsupported chain WHEN getAddress THEN throws`() {
        val factory = GaslessContractAddressFactory(Blockchain.Bitcoin)

        val error = runCatching { factory.getGaslessExecutorContractAddress(isV2 = true) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}