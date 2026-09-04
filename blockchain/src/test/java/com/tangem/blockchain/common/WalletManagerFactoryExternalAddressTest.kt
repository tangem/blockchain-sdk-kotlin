package com.tangem.blockchain.common

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.network.providers.ProviderType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test

/**
 * The provider for an external address is assembled from the chain's RPC providers alone: the address is given, no
 * key is involved, and a chain the factory cannot serve yields no provider rather than a broken one.
 */
internal class WalletManagerFactoryExternalAddressTest {

    private val factory = WalletManagerFactory(
        config = BlockchainSdkConfig(),
        blockchainProviderTypes = mapOf(
            Blockchain.Ethereum to listOf(ProviderType.Public(url = "https://ethereum.example.org/")),
            Blockchain.Polygon to emptyList(),
            Blockchain.Mantle to listOf(ProviderType.Public(url = "https://mantle.example.org/")),
        ),
        accountCreator = mockk(),
        featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false),
        blockchainDataStorage = object : BlockchainDataStorage {
            override suspend fun getOrNull(key: String): String? = null
            override suspend fun store(key: String, value: String) = Unit
            override suspend fun remove(key: String) = Unit
        },
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `GIVEN an EVM chain with providers WHEN create THEN the provider observes the given address`() {
        val actual = factory.createExternalAddressBalanceProvider(
            blockchain = Blockchain.Ethereum,
            address = SAFE_ADDRESS,
        )

        assertThat(actual).isNotNull()
        assertThat(actual!!.address).isEqualTo(SAFE_ADDRESS)
        assertThat(actual.blockchain).isEqualTo(Blockchain.Ethereum)
        assertThat(actual.tokens).isEmpty()
        assertThat(actual.amounts).isEmpty()
    }

    @Test
    fun `GIVEN a non-EVM chain WHEN create THEN no provider`() {
        val actual = factory.createExternalAddressBalanceProvider(
            blockchain = Blockchain.Bitcoin,
            address = SAFE_ADDRESS,
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN an address invalid for the chain WHEN create THEN no provider`() {
        val actual = factory.createExternalAddressBalanceProvider(
            blockchain = Blockchain.Ethereum,
            address = "not-an-address",
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a chain without configured providers WHEN create THEN no provider`() {
        val actual = factory.createExternalAddressBalanceProvider(
            blockchain = Blockchain.Polygon,
            address = SAFE_ADDRESS,
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN an EVM chain with an assembly of its own WHEN create THEN no provider`() {
        val actual = factory.createExternalAddressBalanceProvider(
            blockchain = Blockchain.Mantle,
            address = SAFE_ADDRESS,
        )

        assertThat(actual).isNull()
    }

    private companion object {
        const val SAFE_ADDRESS = "0xe7411b2aB7b76a11ca0E6174697714983fC09Cb2"
    }
}