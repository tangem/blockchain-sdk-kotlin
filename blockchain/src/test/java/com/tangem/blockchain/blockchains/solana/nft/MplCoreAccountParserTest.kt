package com.tangem.blockchain.blockchains.solana.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.nft.MplCoreAccountBytes.FreezePlugin
import com.tangem.blockchain.blockchains.solana.nft.MplCoreAccountBytes.FREEZE_DELEGATE
import com.tangem.blockchain.blockchains.solana.nft.MplCoreAccountBytes.PERMANENT_FREEZE_DELEGATE
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

internal class MplCoreAccountParserTest {

    private val owner = PublicKey("CUfZZHt9nNsSnQUv5jhGGLbRRtudpWvzoSwbJQf7T9PW")
    private val collection = PublicKey("CCryptUfeFSZ3Fgc9FLeKrhLVAP67FSqi1GuVoj9CRac")

    @Test
    fun `collector crypt card has an unfrozen permanent freeze delegate and can be transferred`() {
        val asset = MplCoreAccountParser.parseAsset(SolanaNFTFixtures.coreAssetAccount)

        assertThat(asset).isEqualTo(MplCoreAsset(owner = owner, collection = collection, transferBlock = null))
    }

    @Test
    fun `asset without plugins has no transfer block`() {
        val data = MplCoreAccountBytes.asset(owner = owner, collection = null)

        assertThat(MplCoreAccountParser.parseAsset(data))
            .isEqualTo(MplCoreAsset(owner = owner, collection = null, transferBlock = null))
    }

    @Test
    fun `frozen freeze delegate blocks the transfer`() {
        val data = MplCoreAccountBytes.asset(owner, collection, listOf(FreezePlugin(FREEZE_DELEGATE, frozen = true)))

        assertThat(MplCoreAccountParser.parseAsset(data)?.transferBlock).isEqualTo(MplCoreTransferBlock.Frozen)
    }

    @Test
    fun `frozen permanent freeze delegate blocks the transfer`() {
        val data = MplCoreAccountBytes.asset(
            owner = owner,
            collection = collection,
            freezePlugins = listOf(FreezePlugin(PERMANENT_FREEZE_DELEGATE, frozen = true)),
        )

        assertThat(MplCoreAccountParser.parseAsset(data)?.transferBlock).isEqualTo(MplCoreTransferBlock.Frozen)
    }

    @Test
    fun `external plugin that checks transfers blocks the transfer`() {
        val data = MplCoreAccountBytes.asset(owner, collection, externalTransferCheck = true)

        assertThat(MplCoreAccountParser.parseAsset(data)?.transferBlock)
            .isEqualTo(MplCoreTransferBlock.ExternalCheck)
    }

    @Test
    fun `frozen collection blocks the transfer`() {
        val data = MplCoreAccountBytes.collection(
            updateAuthority = owner,
            freezePlugins = listOf(FreezePlugin(PERMANENT_FREEZE_DELEGATE, frozen = true)),
        )

        assertThat(MplCoreAccountParser.parseCollectionTransferBlock(data).getOrNull())
            .isEqualTo(MplCoreTransferBlock.Frozen)
    }

    @Test
    fun `collection without plugins has no transfer block`() {
        val data = MplCoreAccountBytes.collection(updateAuthority = owner)

        assertThat(MplCoreAccountParser.parseCollectionTransferBlock(data).isSuccess).isTrue()
        assertThat(MplCoreAccountParser.parseCollectionTransferBlock(data).getOrNull()).isNull()
    }

    @Test
    fun `account that is not an asset is rejected`() {
        val data = SolanaNFTFixtures.coreAssetAccount.copyOf().also { it[0] = 5 }

        assertThat(MplCoreAccountParser.parseAsset(data)).isNull()
    }

    @Test
    fun `truncated asset is rejected`() {
        val data = SolanaNFTFixtures.coreAssetAccount.copyOfRange(0, 40)

        assertThat(MplCoreAccountParser.parseAsset(data)).isNull()
    }

    @Test
    fun `asset as collection is rejected`() {
        assertThat(MplCoreAccountParser.parseCollectionTransferBlock(SolanaNFTFixtures.coreAssetAccount).isFailure)
            .isTrue()
    }
}