package com.tangem.blockchain.blockchains.solana.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

/**
 * Checks the pNFT transfer against mainnet transaction
 * `2TN2nVUhFNsikVJj1egPjYqQXytMkR5DpkQk3edkBtKxw5r1cx3QjUjzv6BeJ441M3v778qGpLC4sD3UXJqjVkSG`, which moved
 * mint `7Kny3iBH…` from `A9Jn6HqV…` to `5Y5zMeo3…`: every derived address and the instruction itself must come out
 * exactly as the one that landed.
 */
internal class TokenMetadataProgramTest {

    private val mint = PublicKey("7Kny3iBH1MSjeeFQMpGc2sWsFscyuoosSBHMS67ZoPX4")
    private val owner = PublicKey("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo")
    private val destinationOwner = PublicKey("5Y5zMeo3EtRBt6QFQMmJCyG1xcHfiUDH1buhVryLHarv")
    private val ownerToken = PublicKey("6FW2VaYZVyPk2bSg32WwCeigKTrYN7knWBSkyQ37NLXu")
    private val destinationToken = PublicKey("4ZNPdLsBtxTJQ5BXsFSPYRgsKaYAaaYJRyuK33yXVYp9")
    private val ruleSet = PublicKey("7j2KxTBnZjNuxXEj6m8D7J4jrnNJZCbrVPiBwCyHTasJ")

    private val metadataAccount = SolanaNFTFixtures.pnftMetadataAccount

    @Test
    fun `token accounts are the associated accounts of the mint`() {
        assertThat(ata(owner)).isEqualTo(ownerToken)
        assertThat(ata(destinationOwner)).isEqualTo(destinationToken)
    }

    @Test
    fun `program addresses match the landed transaction`() {
        assertThat(TokenMetadataProgram.metadata(mint).toBase58())
            .isEqualTo("4SCNiHv67kq9LhoXwvGZramY9SVfobvbHgn7x4gF55pg")
        assertThat(TokenMetadataProgram.masterEdition(mint).toBase58())
            .isEqualTo("CEZ54V8s54nG4di5JpSX6AVJ7egkx2NouUBAPCVDAubD")
        assertThat(TokenMetadataProgram.tokenRecord(mint, ownerToken).toBase58())
            .isEqualTo("CvDkKDiDD6TNEQopLtcPKaJcsn5f8X5KCwG7giMG1fyC")
        assertThat(TokenMetadataProgram.tokenRecord(mint, destinationToken).toBase58())
            .isEqualTo("7LKkyrDutQW5uD1o8ttA67GvRXdymGccrU7xF3rNnF9M")
    }

    @Test
    fun `transfer instruction matches the landed transaction`() {
        val instruction = TokenMetadataProgram.transfer(
            mint = mint,
            owner = owner,
            ownerToken = ownerToken,
            destinationOwner = destinationOwner,
            destinationToken = destinationToken,
            ruleSet = ruleSet,
            tokenProgram = SolanaTokenProgram.ID.TOKEN.value,
        )

        assertThat(instruction.programId.toBase58()).isEqualTo("metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s")
        assertThat(instruction.data.toHex()).isEqualTo("3100010000000000000000")
        assertThat(
            instruction.keys.map { Triple(it.publicKey.toBase58(), it.isSigner, it.isWritable) },
        ).containsExactly(
            Triple("6FW2VaYZVyPk2bSg32WwCeigKTrYN7knWBSkyQ37NLXu", false, true),
            Triple("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo", false, false),
            Triple("4ZNPdLsBtxTJQ5BXsFSPYRgsKaYAaaYJRyuK33yXVYp9", false, true),
            Triple("5Y5zMeo3EtRBt6QFQMmJCyG1xcHfiUDH1buhVryLHarv", false, false),
            Triple("7Kny3iBH1MSjeeFQMpGc2sWsFscyuoosSBHMS67ZoPX4", false, false),
            Triple("4SCNiHv67kq9LhoXwvGZramY9SVfobvbHgn7x4gF55pg", false, true),
            Triple("CEZ54V8s54nG4di5JpSX6AVJ7egkx2NouUBAPCVDAubD", false, false),
            Triple("CvDkKDiDD6TNEQopLtcPKaJcsn5f8X5KCwG7giMG1fyC", false, true),
            Triple("7LKkyrDutQW5uD1o8ttA67GvRXdymGccrU7xF3rNnF9M", false, true),
            Triple("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo", true, false),
            Triple("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo", true, true),
            Triple("11111111111111111111111111111111", false, false),
            Triple("Sysvar1nstructions1111111111111111111111111", false, false),
            Triple("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", false, false),
            Triple("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL", false, false),
            Triple("auth9SigNpDKz4sJJ1DfCTuZrZNSAgh9sFD3rboVmgg", false, false),
            Triple("7j2KxTBnZjNuxXEj6m8D7J4jrnNJZCbrVPiBwCyHTasJ", false, false),
        ).inOrder()
    }

    @Test
    fun `without a rule set both authorization accounts are the program itself`() {
        val instruction = TokenMetadataProgram.transfer(
            mint = mint,
            owner = owner,
            ownerToken = ownerToken,
            destinationOwner = destinationOwner,
            destinationToken = destinationToken,
            ruleSet = null,
            tokenProgram = SolanaTokenProgram.ID.TOKEN.value,
        )

        assertThat(instruction.keys.takeLast(2).map { it.publicKey })
            .containsExactly(TokenMetadataProgram.programId, TokenMetadataProgram.programId)
    }

    @Test
    fun `rule set is read from the metadata account`() {
        assertThat(TokenMetadataParser.parse(metadataAccount)?.ruleSet).isEqualTo(ruleSet)
    }

    @Test
    fun `metadata without programmable config has no rule set`() {
        val withoutConfig = metadataAccount.copyOf().also { it[PROGRAMMABLE_CONFIG_OFFSET] = 0 }

        assertThat(TokenMetadataParser.parse(withoutConfig)?.ruleSet).isNull()
    }

    @Test
    fun `programmable config without rule set has no rule set`() {
        val withoutRuleSet = metadataAccount.copyOf().also { it[PROGRAMMABLE_CONFIG_OFFSET + 2] = 0 }

        assertThat(TokenMetadataParser.parse(withoutRuleSet)?.ruleSet).isNull()
    }

    @Test
    fun `truncated metadata has no rule set`() {
        val truncated = metadataAccount.copyOfRange(0, PROGRAMMABLE_CONFIG_OFFSET + 10)

        assertThat(TokenMetadataParser.parse(truncated)?.ruleSet).isNull()
    }

    private fun ata(walletOwner: PublicKey): PublicKey =
        createAssociatedSolanaTokenAddress(walletOwner, mint, SolanaTokenProgram.ID.TOKEN).getOrThrow()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        /** `Option<ProgrammableConfig>` tag, found by walking the account by hand: name, symbol, uri, 5 creators. */
        const val PROGRAMMABLE_CONFIG_OFFSET = 505
    }
}