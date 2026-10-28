package com.tangem.blockchain.nft.providers.moralis.solana

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaNFTAssetResponse
import org.junit.Test

internal class MoralisSolanaLegacyNFTMapperTest {

    private val adapter = Moshi.Builder().build().adapter<List<MoralisSolanaNFTAssetResponse>>(
        Types.newParameterizedType(List::class.java, MoralisSolanaNFTAssetResponse::class.java),
    )
    private val mapper = MoralisSolanaLegacyNFTMapper(Blockchain.Solana)

    @Test
    fun `GIVEN one collection address under various names WHEN map THEN assets form one collection`() {
        // Act
        val collections = mapper.toCollections(parse(MoralisSolanaFixtures.legacyOneAddressVariousNames), "Owner")

        // Assert
        assertThat(collections.map { it.identifier }).containsExactly(NFTCollection.Identifier.Solana("CC"))
        assertThat(collections.single().name).isEqualTo("Collector Crypt")
        assertThat(collections.single().count).isEqualTo(3)
    }

    @Test
    fun `GIVEN empty and missing collection addresses WHEN map THEN assets share the no-collection group`() {
        // Act
        val collections = mapper.toCollections(parse(MoralisSolanaFixtures.legacyAddressless), "Owner")

        // Assert
        assertThat(collections.map { it.identifier }).containsExactly(NFTCollection.Identifier.Solana(null))
        assertThat(collections.single().count).isEqualTo(3)
    }

    @Test
    fun `GIVEN grouped assets with different collection data WHEN map THEN each keeps its own description`() {
        // Act
        val collections = mapper.toCollections(parse(MoralisSolanaFixtures.legacyAddressless), "Owner")

        // Assert
        val descriptions = collections.single().assets.associate { it.name to it.description }
        assertThat(descriptions).containsExactly(
            "Named loose",
            "Named description",
            "Empty address",
            null,
            "No collection",
            null,
        )
    }

    private fun parse(json: String) = requireNotNull(adapter.fromJson(json))
}