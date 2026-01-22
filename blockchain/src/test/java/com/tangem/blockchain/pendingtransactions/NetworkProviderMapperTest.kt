package com.tangem.blockchain.pendingtransactions

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.pendingtransactions.providers.NetworkProviderMapper
import org.junit.Test

internal class NetworkProviderMapperTest {

    private val mapper = NetworkProviderMapper()

    @Test
    fun `GIVEN Public provider WHEN toStorageKey THEN returns Public prefix with url`() {
        val providerType = ProviderType.Public("https://example.com/rpc")

        val result = mapper.toStorageKey(providerType)

        assertThat(result).isEqualTo("Public:https://example.com/rpc")
    }

    @Test
    fun `GIVEN NowNodes provider WHEN toStorageKey THEN returns qualified name`() {
        val providerType = ProviderType.NowNodes

        val result = mapper.toStorageKey(providerType)

        assertThat(result).isEqualTo("com.tangem.blockchain.common.network.providers.ProviderType.NowNodes")
    }

    @Test
    fun `GIVEN GetBlock provider WHEN toStorageKey THEN returns qualified name`() {
        val providerType = ProviderType.GetBlock

        val result = mapper.toStorageKey(providerType)

        assertThat(result).isEqualTo("com.tangem.blockchain.common.network.providers.ProviderType.GetBlock")
    }

    @Test
    fun `GIVEN QuickNode provider WHEN toStorageKey THEN returns qualified name`() {
        val providerType = ProviderType.QuickNode

        val result = mapper.toStorageKey(providerType)

        assertThat(result).isEqualTo("com.tangem.blockchain.common.network.providers.ProviderType.QuickNode")
    }

    @Test
    fun `GIVEN Infura provider WHEN toStorageKey THEN returns qualified name`() {
        val providerType = ProviderType.EthereumLike.Infura

        val result = mapper.toStorageKey(providerType)

        assertThat(result).isEqualTo("com.tangem.blockchain.common.network.providers.ProviderType.EthereumLike.Infura")
    }

    @Test
    fun `GIVEN list with matching Public provider WHEN findProviderTypeByStorageKey THEN returns correct provider`() {
        val providerTypes = listOf(
            ProviderType.Public("https://example1.com"),
            ProviderType.Public("https://example2.com"),
            ProviderType.NowNodes,
        )

        val result = mapper.findProviderTypeByStorageKey("Public:https://example2.com", providerTypes)

        assertThat(result).isEqualTo(ProviderType.Public("https://example2.com"))
    }

    @Test
    fun `GIVEN list with matching private provider WHEN findProviderTypeByStorageKey THEN returns correct provider`() {
        val providerTypes = listOf(
            ProviderType.Public("https://example.com"),
            ProviderType.NowNodes,
            ProviderType.GetBlock,
        )

        val result = mapper.findProviderTypeByStorageKey(
            "com.tangem.blockchain.common.network.providers.ProviderType.NowNodes",
            providerTypes,
        )

        assertThat(result).isEqualTo(ProviderType.NowNodes)
    }

    @Test
    fun `GIVEN list without matching provider WHEN findProviderTypeByStorageKey THEN returns null`() {
        val providerTypes = listOf(
            ProviderType.Public("https://example.com"),
            ProviderType.NowNodes,
        )

        val result = mapper.findProviderTypeByStorageKey("NonExistentProvider", providerTypes)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN empty list WHEN findProviderTypeByStorageKey THEN returns null`() {
        val result = mapper.findProviderTypeByStorageKey("Public:https://example.com", emptyList())

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN Public provider name WHEN isPrivateProvider THEN returns false`() {
        val providerName = "Public:https://example.com"

        val result = mapper.isPrivateProvider(providerName)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN NowNodes provider name WHEN isPrivateProvider THEN returns true`() {
        val providerName = "com.tangem.blockchain.common.network.providers.ProviderType.NowNodes"

        val result = mapper.isPrivateProvider(providerName)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN GetBlock provider name WHEN isPrivateProvider THEN returns true`() {
        val providerName = "com.tangem.blockchain.common.network.providers.ProviderType.GetBlock"

        val result = mapper.isPrivateProvider(providerName)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN Infura provider name WHEN isPrivateProvider THEN returns true`() {
        val providerName = "com.tangem.blockchain.common.network.providers.ProviderType.EthereumLike.Infura"

        val result = mapper.isPrivateProvider(providerName)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN arbitrary non-public name WHEN isPrivateProvider THEN returns true`() {
        val providerName = "SomePrivateProvider"

        val result = mapper.isPrivateProvider(providerName)

        assertThat(result).isTrue()
    }
}