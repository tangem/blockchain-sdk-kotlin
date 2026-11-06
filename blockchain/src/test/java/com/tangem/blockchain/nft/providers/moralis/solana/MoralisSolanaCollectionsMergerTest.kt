package com.tangem.blockchain.nft.providers.moralis.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import org.junit.Test

internal class MoralisSolanaCollectionsMergerTest {

    @Test
    fun `core assets join the legacy collection with the same address`() {
        val legacy = listOf(collection("CC", "pnft"))
        val core = listOf(collection("CC", "core"))

        val merged = MoralisSolanaCollectionsMerger.merge(legacy, core)

        assertThat(merged).hasSize(1)
        assertThat(merged.single().assets.map { it.identifier.stringValue() }).containsExactly("pnft", "core").inOrder()
        assertThat(merged.single().count).isEqualTo(2)
    }

    @Test
    fun `legacy logo is kept when core assets merge into it`() {
        val legacy = listOf(collection("CC", "pnft").copy(logoUrl = "https://legacy.example.com/logo.png"))
        val core = listOf(collection("CC", "core").copy(logoUrl = "https://core.example.com/first-asset.png"))

        val merged = MoralisSolanaCollectionsMerger.merge(legacy, core)

        assertThat(merged.single().logoUrl).isEqualTo("https://legacy.example.com/logo.png")
    }

    @Test
    fun `GIVEN legacy collection without a logo WHEN core assets merge THEN the first core image is its logo`() {
        // Arrange
        val legacy = listOf(collection("CC", "pnft"))
        val core = listOf(collection("CC", "core").copy(logoUrl = "https://core.example.com/first-asset.png"))

        // Act
        val merged = MoralisSolanaCollectionsMerger.merge(legacy, core)

        // Assert
        assertThat(merged.single().logoUrl).isEqualTo("https://core.example.com/first-asset.png")
    }

    @Test
    fun `GIVEN core asset without a description WHEN merged THEN it shows the legacy collection description`() {
        // Arrange
        val legacy = listOf(collection("CC", "pnft").copy(description = "Graded cards"))
        val core = listOf(collection("CC", "core"))

        // Act
        val merged = MoralisSolanaCollectionsMerger.merge(legacy, core)

        // Assert
        assertThat(merged.single().assets.map { it.description }).containsExactly(null, "Graded cards").inOrder()
    }

    @Test
    fun `core collection without a legacy match is appended`() {
        val merged = MoralisSolanaCollectionsMerger.merge(listOf(collection("A", "a")), listOf(collection("B", "b")))

        assertThat(merged.map { it.identifier }).containsExactly(
            NFTCollection.Identifier.Solana("A"),
            NFTCollection.Identifier.Solana("B"),
        ).inOrder()
    }

    @Test
    fun `without core assets legacy collections are returned as is`() {
        val legacy = listOf(collection("A", "a"))

        assertThat(MoralisSolanaCollectionsMerger.merge(legacy, emptyList())).isSameInstanceAs(legacy)
    }

    private fun collection(address: String, asset: String): NFTCollection {
        val identifier = NFTCollection.Identifier.Solana(address)
        val nft = NFTAsset(
            identifier = NFTAsset.Identifier.Solana(asset, null),
            collectionIdentifier = identifier,
            blockchainId = "solana",
            contractType = "",
            owner = "Owner",
            name = asset,
            description = null,
            salePrice = null,
            rarity = null,
            media = null,
            traits = emptyList(),
            amount = 1.toBigInteger(),
            decimals = 0,
        )
        return NFTCollection(
            name = address,
            identifier = identifier,
            blockchainId = "solana",
            description = null,
            logoUrl = null,
            count = 1,
            assets = listOf(nft),
        )
    }

    private fun NFTAsset.Identifier.stringValue() = (this as NFTAsset.Identifier.Solana).tokenAddress
}