package com.tangem.blockchain.common

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.common.extensions.hexToBytes
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test

/**
 * The Safe manager is assembled from the chain's RPC providers around the wallet's real key, but its wallet address
 * is the Safe's; a chain the factory cannot serve for the Safe yields no manager rather than a broken one.
 */
internal class WalletManagerFactorySafeTest {

    private val factory = WalletManagerFactory(
        config = BlockchainSdkConfig(),
        blockchainProviderTypes = mapOf(
            Blockchain.Ethereum to listOf(ProviderType.Public(url = "https://ethereum.example.org/")),
            Blockchain.Polygon to emptyList(),
        ),
        accountCreator = mockk(),
        featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false),
        blockchainDataStorage = object : BlockchainDataStorage {
            override suspend fun getOrNull(key: String): String? = null
            override suspend fun store(key: String, value: String) = Unit
            override suspend fun remove(key: String) = Unit
        },
    )

    private val ownerKey = Wallet.PublicKey(seedKey = OWNER_PUBLIC_KEY.hexToBytes(), derivationType = null)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `GIVEN an EVM chain with providers WHEN create THEN the manager reads for the Safe with the owner key`() {
        val actual = factory.createSafeWalletManager(
            blockchain = Blockchain.Ethereum,
            publicKey = ownerKey,
            safeAddress = SAFE_ADDRESS,
        )

        assertThat(actual).isNotNull()
        assertThat(actual!!.wallet.address).isEqualTo(SAFE_ADDRESS)
        assertThat(actual.wallet.publicKey).isEqualTo(ownerKey)
        assertThat(actual.wallet.blockchain).isEqualTo(Blockchain.Ethereum)
    }

    @Test
    fun `GIVEN a non-EVM chain WHEN create THEN no manager`() {
        val actual = factory.createSafeWalletManager(
            blockchain = Blockchain.Bitcoin,
            publicKey = ownerKey,
            safeAddress = SAFE_ADDRESS,
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN an address invalid for the chain WHEN create THEN no manager`() {
        val actual = factory.createSafeWalletManager(
            blockchain = Blockchain.Ethereum,
            publicKey = ownerKey,
            safeAddress = "not-an-address",
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a chain without configured providers WHEN create THEN no manager`() {
        val actual = factory.createSafeWalletManager(
            blockchain = Blockchain.Polygon,
            publicKey = ownerKey,
            safeAddress = SAFE_ADDRESS,
        )

        assertThat(actual).isNull()
    }

    private companion object {
        const val SAFE_ADDRESS = "0xe7411b2aB7b76a11ca0E6174697714983fC09Cb2"
        const val OWNER_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321998CC2D0" +
            "42901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}