package com.tangem.blockchain.blockchains.solana.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.alt.borsh.SolanaBorshReader
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

internal class TokenMetadataParserTest {

    @Test
    fun `mainnet pnft metadata is programmable`() {
        val metadata = TokenMetadataParser.parse(SolanaNFTFixtures.pnftMetadataAccount)

        assertThat(metadata?.tokenStandard).isEqualTo(4)
        assertThat(metadata?.isProgrammable).isTrue()
    }

    @Test
    fun `metadata with a verified collection and collection details still parses`() {
        val fixture = SolanaNFTFixtures.pnftMetadataAccount

        // `None` collection (1 byte), uses (1 byte) and collection_details (1 byte) sit at offsets 502-504;
        // replacing them with `Some` grows the account, so the tail is spliced in rather than overwritten.
        val collection = byteArrayOf(SOME, VERIFIED) + ByteArray(SolanaBorshReader.PUBLIC_KEY_SIZE)
        val uses = byteArrayOf(NONE)
        val collectionDetails = byteArrayOf(SOME, COLLECTION_DETAILS_V1) + ByteArray(Long.SIZE_BYTES)
        val withCollection = fixture.copyOfRange(0, COLLECTION_OFFSET) +
            collection +
            uses +
            collectionDetails +
            fixture.copyOfRange(PROGRAMMABLE_CONFIG_OFFSET, fixture.size)

        val metadata = TokenMetadataParser.parse(withCollection)

        assertThat(metadata?.tokenStandard).isEqualTo(4)
        assertThat(metadata?.ruleSet).isEqualTo(PublicKey("7j2KxTBnZjNuxXEj6m8D7J4jrnNJZCbrVPiBwCyHTasJ"))
    }

    @Test
    fun `GIVEN pnft metadata with a broken collection WHEN parse THEN token standard is dropped with it`() {
        // Arrange
        val brokenCollection = SolanaNFTFixtures.pnftMetadataAccount.copyOfRange(0, COLLECTION_OFFSET) +
            byteArrayOf(INVALID_OPTION_TAG)

        // Act
        val metadata = TokenMetadataParser.parse(brokenCollection)

        // Assert
        assertThat(metadata).isEqualTo(TokenMetadata(tokenStandard = null, ruleSet = null))
    }

    @Test
    fun `GIVEN collection with an invalid verified flag WHEN parse THEN metadata is not programmable`() {
        // Arrange
        val fixture = SolanaNFTFixtures.pnftMetadataAccount
        val collection = byteArrayOf(SOME, INVALID_BOOL) + ByteArray(SolanaBorshReader.PUBLIC_KEY_SIZE)
        val corrupted = fixture.copyOfRange(0, COLLECTION_OFFSET) +
            collection +
            fixture.copyOfRange(COLLECTION_OFFSET + 1, fixture.size)

        // Act
        val metadata = TokenMetadataParser.parse(corrupted)

        // Assert
        assertThat(metadata?.tokenStandard).isNull()
        assertThat(metadata?.isProgrammable).isFalse()
    }

    @Test
    fun `GIVEN unknown token standard WHEN parse THEN token standard is dropped`() {
        // Arrange
        val corrupted = SolanaNFTFixtures.pnftMetadataAccount.copyOf().apply {
            this[TOKEN_STANDARD_VALUE_OFFSET] = UNKNOWN_TOKEN_STANDARD
        }

        // Act
        val metadata = TokenMetadataParser.parse(corrupted)

        // Assert
        assertThat(metadata?.tokenStandard).isNull()
    }

    @Test
    fun `account that is not metadata is rejected`() {
        val data = SolanaNFTFixtures.pnftMetadataAccount.copyOf().also { it[0] = 11 }

        assertThat(TokenMetadataParser.parse(data)).isNull()
    }

    @Test
    fun `token record states are read`() {
        assertThat(TokenMetadataParser.parseTokenRecordState(byteArrayOf(11, -1, 0)))
            .isEqualTo(TokenRecordState.Unlocked)
        assertThat(TokenMetadataParser.parseTokenRecordState(byteArrayOf(11, -1, 1)))
            .isEqualTo(TokenRecordState.Locked)
        assertThat(TokenMetadataParser.parseTokenRecordState(byteArrayOf(11, -1, 2)))
            .isEqualTo(TokenRecordState.Listed)
    }

    @Test
    fun `unknown token record is rejected`() {
        assertThat(TokenMetadataParser.parseTokenRecordState(byteArrayOf(4, -1, 0))).isNull()
        assertThat(TokenMetadataParser.parseTokenRecordState(byteArrayOf(11, -1, 9))).isNull()
    }

    private companion object {
        const val SOME: Byte = 1
        const val NONE: Byte = 0
        const val VERIFIED: Byte = 1
        const val COLLECTION_DETAILS_V1: Byte = 0
        const val INVALID_OPTION_TAG: Byte = 7
        const val INVALID_BOOL: Byte = 2
        const val UNKNOWN_TOKEN_STANDARD: Byte = 9

        /** Offset of the `token_standard` value, after its `Some` tag at 500. */
        const val TOKEN_STANDARD_VALUE_OFFSET = 501

        /** Offset of the `collection: Option<Collection>` tag in [SolanaNFTFixtures.pnftMetadataAccount]. */
        const val COLLECTION_OFFSET = 502

        /** Offset of the `programmable_config` tag, right after `collection`/`uses`/`collection_details`. */
        const val PROGRAMMABLE_CONFIG_OFFSET = 505
    }
}