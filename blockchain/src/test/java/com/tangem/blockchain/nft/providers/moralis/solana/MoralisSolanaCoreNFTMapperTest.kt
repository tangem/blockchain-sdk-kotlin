package com.tangem.blockchain.nft.providers.moralis.solana

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisWalletNFTsResponse
import org.junit.Test

internal class MoralisSolanaCoreNFTMapperTest {

    private val adapter = Moshi.Builder().build().adapter(MoralisWalletNFTsResponse::class.java)
    private val mapper = MoralisSolanaCoreNFTMapper(Blockchain.Solana)

    private val collections = mapper.toCollections(parse(MoralisSolanaFixtures.coreResponse), owner = "Owner")

    @Test
    fun `only core assets with an address are mapped and grouped by collection address`() {
        assertThat(collections.map { it.identifier }).containsExactly(
            NFTCollection.Identifier.Solana("CoreCollection11111111111111111111111111111"),
            NFTCollection.Identifier.Solana(null),
        )
        assertThat(collections.flatMap { it.assets }.map { it.identifier }).containsExactly(
            NFTAsset.Identifier.Solana("CoreAsset1111111111111111111111111111111111", "MplCore"),
            NFTAsset.Identifier.Solana("CoreAsset2222222222222222222222222222222222", "MplCore"),
        )
    }

    @Test
    fun `asset carries name description traits and an image`() {
        val asset = collections.first().assets.single()

        assertThat(collections.first().name).isEqualTo("Collector Crypt")
        assertThat(asset.name).isEqualTo("Card #1")
        assertThat(asset.description).isEqualTo("A graded card")
        assertThat(asset.amount).isEqualTo(1.toBigInteger())
        assertThat(asset.decimals).isEqualTo(0)
        assertThat(asset.media?.imageUrl).isEqualTo("https://example.com/1.png")
        assertThat(asset.traits.map { it.name to it.value })
            .containsExactly("Background" to "Purple", "Grade" to "10")
    }

    @Test
    fun `video asset keeps the metadata image as preview`() {
        val asset = collections.last().assets.single()

        assertThat(asset.media?.imageUrl).isEqualTo("https://ipfs.io/ipfs/QmImage/2.png")
    }

    @Test
    fun `collection logo is the first asset's image`() {
        assertThat(collections.first().logoUrl).isEqualTo("https://example.com/1.png")
        assertThat(collections.last().logoUrl).isEqualTo("https://ipfs.io/ipfs/QmImage/2.png")
    }

    @Test
    fun `GIVEN image media without resized copies WHEN map THEN the original media is used before metadata`() {
        // Arrange
        val items = parse(
            """
            {
              "result": [
                {
                  "tokenAddress": "CoreAsset1111111111111111111111111111111111",
                  "tokenStandard": "MplCore",
                  "metadata": { "image": "https://example.com/metadata.png" },
                  "media": { "mimetype": "image/png", "originalMediaUrl": "https://example.com/media.png?w=1" }
                }
              ]
            }
            """.trimIndent(),
        )

        // Act
        val asset = mapper.toCollections(items, owner = "Owner").single().assets.single()

        // Assert
        assertThat(asset.media?.imageUrl).isEqualTo("https://example.com/media.png")
    }

    @Test
    fun `GIVEN an empty original media WHEN map THEN the metadata image is used before the resized copies`() {
        // Arrange
        val items = parse(
            """
            {
              "result": [
                {
                  "tokenAddress": "CoreAsset1111111111111111111111111111111111",
                  "tokenStandard": "MplCore",
                  "metadata": { "image": "https://example.com/metadata.png" },
                  "media": {
                    "mimetype": "image/png",
                    "originalMediaUrl": "",
                    "mediaCollection": { "medium": { "url": "https://media.moralis.io/medium.png" } }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        // Act
        val asset = mapper.toCollections(items, owner = "Owner").single().assets.single()

        // Assert
        assertThat(asset.media?.imageUrl).isEqualTo("https://example.com/metadata.png")
    }

    @Test
    fun `GIVEN only resized copies WHEN map THEN the largest copy is used`() {
        // Arrange
        val items = parse(
            """
            {
              "result": [
                {
                  "tokenAddress": "CoreAsset1111111111111111111111111111111111",
                  "tokenStandard": "MplCore",
                  "metadata": { "attributes": [] },
                  "media": {
                    "mimetype": "image/png",
                    "originalMediaUrl": null,
                    "mediaCollection": {
                      "low": { "url": "https://media.moralis.io/low.png" },
                      "medium": { "url": "https://media.moralis.io/medium.png" }
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        // Act
        val asset = mapper.toCollections(items, owner = "Owner").single().assets.single()

        // Assert
        assertThat(asset.media?.imageUrl).isEqualTo("https://media.moralis.io/medium.png")
    }

    @Test
    fun `GIVEN no media WHEN map THEN the metadata image is used`() {
        // Arrange
        val items = parse(
            """
            {
              "result": [
                {
                  "tokenAddress": "CoreAsset1111111111111111111111111111111111",
                  "tokenStandard": "MplCore",
                  "metadata": { "image": "https://example.com/metadata.png" },
                  "media": null
                }
              ]
            }
            """.trimIndent(),
        )

        // Act
        val asset = mapper.toCollections(items, owner = "Owner").single().assets.single()

        // Assert
        assertThat(asset.media?.imageUrl).isEqualTo("https://example.com/metadata.png")
    }

    @Test
    fun `GIVEN an empty collection address WHEN map THEN the asset goes to the no-collection group`() {
        // Arrange
        val response = """
        {
          "result": [
            { "tokenAddress": "CoreAsset3", "tokenStandard": "MplCore", "collection": { "address": "", "name": "Empty" } },
            { "tokenAddress": "CoreAsset4", "tokenStandard": "MplCore" }
          ]
        }
        """.trimIndent()

        // Act
        val collections = mapper.toCollections(parse(response), owner = "Owner")

        // Assert
        assertThat(collections.map { it.identifier }).containsExactly(NFTCollection.Identifier.Solana(null))
        assertThat(collections.single().count).isEqualTo(2)
    }

    @Test
    fun `GIVEN an empty token address WHEN map THEN the asset is dropped`() {
        // Arrange
        val response = """
        { "result": [{ "tokenAddress": "", "tokenStandard": "MplCore", "name": "No id" }] }
        """.trimIndent()

        // Act
        val collections = mapper.toCollections(parse(response), owner = "Owner")

        // Assert
        assertThat(collections).isEmpty()
    }

    @Test
    fun `GIVEN an empty name WHEN map THEN the metadata name is used`() {
        // Arrange
        val response = """
        {
          "result": [
            { "tokenAddress": "CoreAsset5", "tokenStandard": "MplCore", "name": "", "metadata": { "name": "Card #5" } }
          ]
        }
        """.trimIndent()

        // Act
        val asset = mapper.toCollections(parse(response), owner = "Owner").single().assets.single()

        // Assert
        assertThat(asset.name).isEqualTo("Card #5")
    }

    private fun parse(json: String): List<MoralisWalletNFTsResponse.Item> =
        requireNotNull(adapter.fromJson(json)).result.orEmpty()
}