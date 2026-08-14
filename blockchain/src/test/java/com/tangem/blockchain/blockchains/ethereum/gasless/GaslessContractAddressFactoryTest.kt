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
    fun `GIVEN supported chains WHEN v1 THEN returns the v1 production executor address`() {
        val expectedAddresses = mapOf(
            Blockchain.Ethereum to "0xe3014E9AB2739aDeF234B3829C79128746160178",
            Blockchain.BSC to "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3",
            Blockchain.Polygon to "0x2C2397c7605dc6d5493518260BDdeebE743B3faD",
            Blockchain.Arbitrum to "0x20e7016ff14Dd10f04028fE52aBBca34F44b6965",
            Blockchain.Base to "0x61dD8620410a2372CbE4946f9148671F38F93fC7",
        )

        expectedAddresses.forEach { (blockchain, expected) ->
            val factory = GaslessContractAddressFactory(blockchain)

            assertThat(factory.getGaslessExecutorContractAddress(isV2 = false)).isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN supported chains WHEN v2 THEN returns the v2 production executor address`() {
        val expectedAddresses = mapOf(
            Blockchain.Ethereum to "0xb94B392b61c16Ddb7118849D4970570C07F75dD1",
            Blockchain.BSC to "0x96922f4b701F0138064bCcB1549B4B7B6b3447CC",
            Blockchain.Polygon to "0x02a35743C4170A3685271708399311801a230cf0",
            Blockchain.Arbitrum to "0x4E039670C679346f785D61a0e21aBe0330F1b776",
            Blockchain.Base to "0xA787dd893e772c42cCe545A2560D53AcdDe251A6",
        )

        expectedAddresses.forEach { (blockchain, expected) ->
            val factory = GaslessContractAddressFactory(blockchain)

            assertThat(factory.getGaslessExecutorContractAddress(isV2 = true)).isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN supported chains WHEN comparing versions THEN v1 and v2 addresses differ`() {
        val supportedChains = listOf(
            Blockchain.Ethereum,
            Blockchain.BSC,
            Blockchain.Polygon,
            Blockchain.Arbitrum,
            Blockchain.Base,
        )

        supportedChains.forEach { blockchain ->
            val factory = GaslessContractAddressFactory(blockchain)

            assertThat(factory.getGaslessExecutorContractAddress(isV2 = true))
                .isNotEqualTo(factory.getGaslessExecutorContractAddress(isV2 = false))
        }
    }

    @Test
    fun `GIVEN unsupported chain WHEN getAddress THEN throws`() {
        val factory = GaslessContractAddressFactory(Blockchain.Bitcoin)

        val v2Error = runCatching { factory.getGaslessExecutorContractAddress(isV2 = true) }.exceptionOrNull()
        val v1Error = runCatching { factory.getGaslessExecutorContractAddress(isV2 = false) }.exceptionOrNull()

        assertThat(v2Error).isInstanceOf(IllegalStateException::class.java)
        assertThat(v1Error).isInstanceOf(IllegalStateException::class.java)
    }
}