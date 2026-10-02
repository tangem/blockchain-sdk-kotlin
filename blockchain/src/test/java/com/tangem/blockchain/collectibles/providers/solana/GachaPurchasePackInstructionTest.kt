package com.tangem.blockchain.collectibles.providers.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaMemoProgram
import com.tangem.blockchain.collectibles.providers.solana.instruction.GachaPurchasePackInstruction
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

internal class GachaPurchasePackInstructionTest {

    @Test
    fun `GIVEN amount fee and memo WHEN creating purchase_pack THEN data is the discriminator then borsh args`() {
        // Act
        val instruction = createInstruction(amount = 250_000_000L, feeAmount = 2_500_000L, memo = "abc")

        // Assert
        // sha256("global:purchase_pack")[0..8] ‖ amount u64 LE ‖ fee_amount u64 LE ‖ string
        val expected = byteArrayOf(43, 116, -121, 75, -5, 67, 81, 26) +
            byteArrayOf(-128, -78, -26, 14, 0, 0, 0, 0) +
            byteArrayOf(-96, 37, 38, 0, 0, 0, 0, 0) +
            byteArrayOf(3, 0, 0, 0) +
            "abc".toByteArray()
        assertThat(instruction.data).isEqualTo(expected)
        assertThat(instruction.programId).isEqualTo(GachaVaultProgram.programId)
    }

    @Test
    fun `GIVEN purchase_pack WHEN listing accounts THEN they follow the IDL order and flags`() {
        // Act
        val instruction = createInstruction(amount = 1L, feeAmount = 0L, memo = "m")

        // Assert
        val actual = instruction.keys.map { Triple(it.publicKey.toBase58(), it.isSigner, it.isWritable) }
        assertThat(actual).containsExactly(
            Triple(CONFIG, false, false),
            Triple(VAULT, false, true),
            Triple(SESSION_KEY, true, false),
            Triple(USDC_MINT, false, false),
            Triple(VAULT_USDC, false, true),
            Triple(GACHA_USDC, false, true),
            Triple(FEE_USDC, false, true),
            Triple(TOKEN_PROGRAM, false, false),
        ).inOrder()
    }

    @Test
    fun `GIVEN a memo WHEN creating the memo instruction THEN it carries the utf8 string and no accounts`() {
        // Act
        val instruction = SolanaMemoProgram.createMemoInstruction("slug-uuid:open")

        // Assert
        assertThat(instruction.programId.toBase58()).isEqualTo("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        assertThat(instruction.keys).isEmpty()
        assertThat(instruction.data).isEqualTo("slug-uuid:open".toByteArray())
    }

    private fun createInstruction(amount: Long, feeAmount: Long, memo: String) = GachaPurchasePackInstruction.create(
        config = PublicKey(CONFIG),
        vault = PublicKey(VAULT),
        sessionKey = PublicKey(SESSION_KEY),
        usdcMint = PublicKey(USDC_MINT),
        vaultUsdc = PublicKey(VAULT_USDC),
        gachaUsdc = PublicKey(GACHA_USDC),
        feeUsdc = PublicKey(FEE_USDC),
        amount = amount,
        feeAmount = feeAmount,
        memo = memo,
    )

    private companion object {
        const val CONFIG = "53EDbBLB1sQuVv6aH7us9TQ7XSGXFijPmPdAGNcY8Y1d"
        const val VAULT = "aBRHcE97NY1MjVqGbc2LX2cApfPg9r5h5T1KNZS93oA"
        const val SESSION_KEY = "CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31"
        const val USDC_MINT = "Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr"
        const val VAULT_USDC = "6atEYZQxh5sYXjnwS5WfVvnh6p8wUXibc2xcD2Gftfqs"
        const val GACHA_USDC = "9ZSgA3PMjeAU8K6CWzgJ8oDnhZwwSgHZu6KP3X95t7Jq"
        const val FEE_USDC = "9qsWLjBgicHNncaM1qtLb4jqj4a4NixMwvNXNmsqQyV9"
        const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    }
}