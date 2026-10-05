package com.tangem.blockchain.blockchains.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.alt.patchComputeBudgetProgramInstructions
import foundation.metaplex.solana.transactions.TransactionInstruction
import foundation.metaplex.solanapublickeys.PublicKey
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SolanaComputeBudgetPatchTest {

    @Test
    fun `regular limit is increased by 20 percent`() {
        val patched = listOf(setComputeUnitLimit(200_000L)).patchComputeBudgetProgramInstructions()

        assertThat(patched.single().computeUnitLimit()).isEqualTo(240_000L)
    }

    @Test
    fun `limit that would overflow int after increase is clamped to solana max`() {
        val patched = listOf(setComputeUnitLimit(2_000_000_000L)).patchComputeBudgetProgramInstructions()

        assertThat(patched.single().computeUnitLimit()).isEqualTo(1_400_000L)
    }

    @Test
    fun `limit above int max is read as unsigned and clamped to solana max`() {
        val patched = listOf(setComputeUnitLimit(0xFFFF_FFFFL)).patchComputeBudgetProgramInstructions()

        assertThat(patched.single().computeUnitLimit()).isEqualTo(1_400_000L)
    }

    @Test
    fun `limit already at solana max stays at solana max`() {
        val patched = listOf(setComputeUnitLimit(1_400_000L)).patchComputeBudgetProgramInstructions()

        assertThat(patched.single().computeUnitLimit()).isEqualTo(1_400_000L)
    }

    @Test
    fun `other five byte compute budget instructions are left untouched`() {
        val requestHeapFrame = computeBudgetInstruction(discriminator = 0x01, operand = 256_000L)
        val setLoadedAccountsDataSizeLimit = computeBudgetInstruction(discriminator = 0x04, operand = 2_000_000_000L)

        val patched = listOf(requestHeapFrame, setLoadedAccountsDataSizeLimit).patchComputeBudgetProgramInstructions()

        assertThat(patched.map { it.data.toList() })
            .containsExactly(requestHeapFrame.data.toList(), setLoadedAccountsDataSizeLimit.data.toList())
            .inOrder()
    }

    private fun setComputeUnitLimit(units: Long): TransactionInstruction {
        return computeBudgetInstruction(discriminator = 0x02, operand = units)
    }

    private fun computeBudgetInstruction(discriminator: Byte, operand: Long): TransactionInstruction {
        val data = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
            .put(discriminator)
            .putInt(operand.toInt())
            .array()
        return TransactionInstruction(
            programId = PublicKey("ComputeBudget111111111111111111111111111111"),
            keys = emptyList(),
            data = data,
        )
    }

    private fun TransactionInstruction.computeUnitLimit(): Long {
        return ByteBuffer.wrap(data, 1, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt().toLong()
    }
}