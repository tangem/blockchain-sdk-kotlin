package com.tangem.blockchain.blockchains.solana.solanaj.program

import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Solana Compute Budget Program
 *
 * @see <a href="https://solana.com/docs/core/runtime#compute-budget-program">Documentation</a>
 * @see <a href="https://github.com/skynetcap/solanaj/blob/main/src/main/java/org/p2p/solanaj/programs/ComputeBudgetProgram.java">ComputeBudgetProgram.java</a>
 * */
internal object SolanaComputeBudgetProgram {

    private val programId: PublicKey = PublicKey("ComputeBudget111111111111111111111111111111")

    fun setComputeUnitPrice(microLamports: Long): TransactionInstruction {
        val transactionData = encodeSetComputeUnitPriceTransaction(microLamports)

        return Program.createTransactionInstruction(programId, emptyList<AccountMeta>(), transactionData)
    }

    fun setComputeUnitLimit(units: Int): TransactionInstruction {
        val transactionData = encodeSetComputeUnitLimitTransaction(units)

        return Program.createTransactionInstruction(programId, emptyList<AccountMeta>(), transactionData)
    }

    @Suppress("MagicNumber")
    private fun encodeSetComputeUnitPriceTransaction(microLamports: Long): ByteArray {
        val result = ByteBuffer.allocate(9).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(0, 0x03.toByte())
            putLong(1, microLamports)
        }

        return result.array()
    }

    @Suppress("MagicNumber")
    private fun encodeSetComputeUnitLimitTransaction(units: Int): ByteArray {
        val result = ByteBuffer.allocate(5).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(0, 0x02.toByte())
            putInt(1, units)
        }

        return result.array()
    }
}