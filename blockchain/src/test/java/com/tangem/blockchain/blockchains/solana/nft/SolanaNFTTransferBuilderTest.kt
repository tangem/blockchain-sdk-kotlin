package com.tangem.blockchain.blockchains.solana.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.nft.MplCoreAccountBytes.FreezePlugin
import com.tangem.blockchain.blockchains.solana.nft.MplCoreAccountBytes.FREEZE_DELEGATE
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

internal class SolanaNFTTransferBuilderTest {

    private val wallet = PublicKey("CUfZZHt9nNsSnQUv5jhGGLbRRtudpWvzoSwbJQf7T9PW")
    private val recipient = PublicKey("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo")
    private val coreAsset = PublicKey("3xYi4gjZ8HKp8N2yEHiYKjVrTKX3K9KcC33qYxWzbDFZ")
    private val coreCollection = PublicKey("CCryptUfeFSZ3Fgc9FLeKrhLVAP67FSqi1GuVoj9CRac")
    private val pnftMint = PublicKey("7Kny3iBH1MSjeeFQMpGc2sWsFscyuoosSBHMS67ZoPX4")
    private val ruleSet = PublicKey("7j2KxTBnZjNuxXEj6m8D7J4jrnNJZCbrVPiBwCyHTasJ")

    private val core = MplCoreProgram.programId.toBase58()
    private val tokenProgram = SolanaTokenProgram.ID.TOKEN.value.toBase58()
    private val token2022 = SolanaTokenProgram.ID.TOKEN_2022.value.toBase58()
    private val metadataProgram = TokenMetadataProgram.programId.toBase58()
    private val systemProgram = "11111111111111111111111111111111"

    @Test
    fun `GIVEN own core asset in a collection WHEN route THEN core transfer with the on-chain collection`() = runTest {
        // Arrange
        val builder = builder(
            coreAsset to SolanaNFTAccount(core, MplCoreAccountBytes.asset(wallet, coreCollection)),
            coreCollection to SolanaNFTAccount(core, MplCoreAccountBytes.collection(wallet)),
        )

        // Act
        val route = builder.route(coreAsset.toBase58(), recipient.toBase58()).successValue()

        // Assert
        route as SolanaNFTTransferRoute.Program
        assertThat(route.isCore).isTrue()
        assertThat(route.computeUnitLimit).isEqualTo(50_000)
        assertThat(route.createdAccountSpaces).isEmpty()
        assertThat(route.instructions.single().keys[1].publicKey).isEqualTo(coreCollection)
    }

    @Test
    fun `GIVEN collector crypt card from mainnet WHEN route THEN core transfer`() = runTest {
        // Arrange
        val builder = builder(
            coreAsset to SolanaNFTAccount(core, SolanaNFTFixtures.coreAssetAccount),
            coreCollection to SolanaNFTAccount(core, MplCoreAccountBytes.collection(wallet)),
        )

        // Act
        val route = builder.route(coreAsset.toBase58(), recipient.toBase58()).successValue()

        // Assert
        assertThat((route as SolanaNFTTransferRoute.Program).isCore).isTrue()
    }

    @Test
    fun `GIVEN core asset of another wallet WHEN route THEN not owner`() = runTest {
        val builder = builder(coreAsset to SolanaNFTAccount(core, MplCoreAccountBytes.asset(recipient, null)))

        assertThat(builder.route(coreAsset.toBase58(), wallet.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTNotOwner)
    }

    @Test
    fun `GIVEN frozen core asset WHEN route THEN frozen`() = runTest {
        val frozen = MplCoreAccountBytes.asset(wallet, null, listOf(FreezePlugin(FREEZE_DELEGATE, frozen = true)))
        val builder = builder(coreAsset to SolanaNFTAccount(core, frozen))

        assertThat(builder.route(coreAsset.toBase58(), recipient.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTFrozen)
    }

    @Test
    fun `GIVEN frozen collection WHEN route THEN frozen`() = runTest {
        val frozenCollection = MplCoreAccountBytes.collection(
            updateAuthority = wallet,
            freezePlugins = listOf(FreezePlugin(FREEZE_DELEGATE, frozen = true)),
        )
        val builder = builder(
            coreAsset to SolanaNFTAccount(core, MplCoreAccountBytes.asset(wallet, coreCollection)),
            coreCollection to SolanaNFTAccount(core, frozenCollection),
        )

        assertThat(builder.route(coreAsset.toBase58(), recipient.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTFrozen)
    }

    @Test
    fun `GIVEN core asset with transfer checking plugin WHEN route THEN unsupported`() = runTest {
        val checked = MplCoreAccountBytes.asset(wallet, null, externalTransferCheck = true)
        val builder = builder(coreAsset to SolanaNFTAccount(core, checked))

        assertThat(builder.route(coreAsset.toBase58(), recipient.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTUnsupportedAsset)
    }

    @Test
    fun `GIVEN asset itself as recipient WHEN route THEN invalid destination`() = runTest {
        val builder = builder(coreAsset to SolanaNFTAccount(core, MplCoreAccountBytes.asset(wallet, null)))

        assertThat(builder.route(coreAsset.toBase58(), coreAsset.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTInvalidDestination)
    }

    @Test
    fun `GIVEN unlocked pnft and fresh recipient WHEN route THEN pnft transfer with rule set and two new accounts`() =
        runTest {
            // Arrange
            val ownerToken = ata(wallet)
            val builder = builder(
                pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
                TokenMetadataProgram.metadata(pnftMint) to
                    SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
                TokenMetadataProgram.tokenRecord(pnftMint, ownerToken) to
                    SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 0)),
            )

            // Act
            val route = builder.route(pnftMint.toBase58(), recipient.toBase58()).successValue()

            // Assert
            route as SolanaNFTTransferRoute.Program
            assertThat(route.isCore).isFalse()
            assertThat(route.computeUnitLimit).isEqualTo(200_000)
            assertThat(route.createdAccountSpaces).containsExactly(165L, 80L).inOrder()
            val instructionKeys = route.instructions.single().keys
            assertThat(instructionKeys.last().publicKey).isEqualTo(ruleSet)
        }

    @Test
    fun `GIVEN recipient already holding the mint WHEN route THEN no new accounts`() = runTest {
        // Arrange
        val ownerToken = ata(wallet)
        val recipientToken = ata(recipient)
        val builder = builder(
            pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
            TokenMetadataProgram.tokenRecord(pnftMint, ownerToken) to
                SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 0)),
            recipientToken to SolanaNFTAccount(tokenProgram, ByteArray(165)),
            TokenMetadataProgram.tokenRecord(pnftMint, recipientToken) to
                SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 0)),
        )

        // Act
        val route = builder.route(pnftMint.toBase58(), recipient.toBase58()).successValue()

        // Assert
        assertThat((route as SolanaNFTTransferRoute.Program).createdAccountSpaces).isEmpty()
    }

    @Test
    fun `GIVEN recipient token account without token record WHEN route THEN only the record is created`() = runTest {
        // Arrange
        val builder = builder(
            pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
            TokenMetadataProgram.tokenRecord(pnftMint, ata(wallet)) to
                SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 0)),
            ata(recipient) to SolanaNFTAccount(tokenProgram, ByteArray(165)),
        )

        // Act
        val route = builder.route(pnftMint.toBase58(), recipient.toBase58()).successValue()

        // Assert
        assertThat((route as SolanaNFTTransferRoute.Program).createdAccountSpaces).containsExactly(80L)
    }

    @Test
    fun `GIVEN listed pnft WHEN route THEN locked`() = runTest {
        val builder = builder(
            pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
            TokenMetadataProgram.tokenRecord(pnftMint, ata(wallet)) to
                SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 2)),
        )

        assertThat(builder.route(pnftMint.toBase58(), recipient.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTLocked)
    }

    @Test
    fun `GIVEN pnft without owner token record WHEN route THEN not owner`() = runTest {
        val builder = builder(
            pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
        )

        assertThat(builder.route(pnftMint.toBase58(), recipient.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTNotOwner)
    }

    @Test
    fun `GIVEN pnft sent to own wallet WHEN route THEN invalid destination`() = runTest {
        val builder = builder(
            pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
        )

        assertThat(builder.route(pnftMint.toBase58(), wallet.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTInvalidDestination)
    }

    @Test
    fun `GIVEN wallet recipient with a system account WHEN route core THEN core transfer`() = runTest {
        // Arrange
        val builder = builder(
            coreAsset to SolanaNFTAccount(core, MplCoreAccountBytes.asset(wallet, null)),
            recipient to SolanaNFTAccount(systemProgram, ByteArray(0)),
        )

        // Act
        val route = builder.route(coreAsset.toBase58(), recipient.toBase58()).successValue()

        // Assert
        assertThat((route as SolanaNFTTransferRoute.Program).isCore).isTrue()
    }

    @Test
    fun `GIVEN token account as recipient WHEN route core THEN invalid destination`() = runTest {
        // Arrange
        val builder = builder(
            coreAsset to SolanaNFTAccount(core, MplCoreAccountBytes.asset(wallet, null)),
            recipient to SolanaNFTAccount(tokenProgram, ByteArray(165)),
        )

        // Act
        val error = builder.route(coreAsset.toBase58(), recipient.toBase58()).failureError()

        // Assert
        assertThat(error).isEqualTo(BlockchainSdkError.Solana.NFTInvalidDestination)
    }

    @Test
    fun `GIVEN token account of the same mint as recipient WHEN route pnft THEN invalid destination`() = runTest {
        // Arrange
        val recipientToken = ata(recipient)
        val builder = builder(
            pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
            TokenMetadataProgram.tokenRecord(pnftMint, ata(wallet)) to
                SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 0)),
            recipientToken to SolanaNFTAccount(tokenProgram, ByteArray(165)),
        )

        // Act
        val error = builder.route(pnftMint.toBase58(), recipientToken.toBase58()).failureError()

        // Assert
        assertThat(error).isEqualTo(BlockchainSdkError.Solana.NFTInvalidDestination)
    }

    @Test
    fun `GIVEN token mint without metadata WHEN route THEN spl`() = runTest {
        val builder = builder(pnftMint to SolanaNFTAccount(tokenProgram, ByteArray(82)))

        assertThat(builder.route(pnftMint.toBase58(), recipient.toBase58()).successValue())
            .isEqualTo(SolanaNFTTransferRoute.Spl)
    }

    @Test
    fun `GIVEN token 2022 mint without metadata WHEN route THEN spl`() = runTest {
        val builder = builder(pnftMint to SolanaNFTAccount(token2022, ByteArray(82)))

        assertThat(builder.route(pnftMint.toBase58(), recipient.toBase58()).successValue())
            .isEqualTo(SolanaNFTTransferRoute.Spl)
    }

    @Test
    fun `GIVEN token 2022 pnft WHEN route THEN pnft transfer through token 2022 accounts`() = runTest {
        // Arrange
        val token2022Program = SolanaTokenProgram.ID.TOKEN_2022
        val ownerToken = ata(wallet, token2022Program)
        val builder = builder(
            pnftMint to SolanaNFTAccount(token2022, ByteArray(82)),
            TokenMetadataProgram.metadata(pnftMint) to
                SolanaNFTAccount(metadataProgram, SolanaNFTFixtures.pnftMetadataAccount),
            TokenMetadataProgram.tokenRecord(pnftMint, ownerToken) to
                SolanaNFTAccount(metadataProgram, byteArrayOf(11, -1, 0)),
        )

        // Act
        val route = builder.route(pnftMint.toBase58(), recipient.toBase58()).successValue()

        // Assert
        route as SolanaNFTTransferRoute.Program
        assertThat(route.createdAccountSpaces).containsExactly(170L, 80L).inOrder()
        val instructionKeys = route.instructions.single().keys.map { it.publicKey }
        assertThat(instructionKeys[0]).isEqualTo(ownerToken)
        assertThat(instructionKeys[2]).isEqualTo(ata(recipient, token2022Program))
        assertThat(instructionKeys[TOKEN_PROGRAM_KEY_INDEX]).isEqualTo(token2022Program.value)
    }

    @Test
    fun `GIVEN missing or foreign asset account WHEN route THEN unsupported`() = runTest {
        assertThat(builder().route(coreAsset.toBase58(), recipient.toBase58()).failureError())
            .isEqualTo(BlockchainSdkError.Solana.NFTUnsupportedAsset)
        assertThat(
            builder(coreAsset to SolanaNFTAccount(recipient.toBase58(), ByteArray(1)))
                .route(coreAsset.toBase58(), recipient.toBase58()).failureError(),
        ).isEqualTo(BlockchainSdkError.Solana.NFTUnsupportedAsset)
    }

    private fun builder(vararg accounts: Pair<PublicKey, SolanaNFTAccount>) = SolanaNFTTransferBuilder(
        account = wallet,
        networkService = FakeNetworkService(accounts.associate { (key, account) -> key.toBase58() to account }),
    )

    private fun ata(owner: PublicKey, tokenProgram: SolanaTokenProgram.ID = SolanaTokenProgram.ID.TOKEN): PublicKey =
        createAssociatedSolanaTokenAddress(owner, pnftMint, tokenProgram).getOrThrow()

    private fun <T> Result<T>.successValue(): T = (this as Result.Success).data

    private fun Result<*>.failureError() = (this as Result.Failure).error

    private class FakeNetworkService(private val accounts: Map<String, SolanaNFTAccount>) : SolanaNFTNetworkService {
        override suspend fun readAccount(address: PublicKey): Result<SolanaNFTAccount?> =
            Result.Success(accounts[address.toBase58()])

        override suspend fun rentExemption(space: Long): Result<Long> = Result.Success(space)
    }

    private companion object {
        const val TOKEN_PROGRAM_KEY_INDEX = 13
    }
}