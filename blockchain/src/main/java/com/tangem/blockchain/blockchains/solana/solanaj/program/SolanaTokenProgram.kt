package com.tangem.blockchain.blockchains.solana.solanaj.program

import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram.ID.TOKEN
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram.ID.TOKEN_2022
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object SolanaTokenProgram {

    /**
     * Same as [org.p2p.solanaj.programs.TokenProgram.transferChecked] but with [programId]
     * */
    @Suppress("LongParameterList")
    internal fun createTransferCheckedInstruction(
        source: PublicKey?,
        destination: PublicKey?,
        amount: Long,
        decimals: Byte,
        owner: PublicKey?,
        tokenMint: PublicKey?,
        programId: ID,
    ): TransactionInstruction {
        val keys = buildList {
            add(AccountMeta(source, false, true))
            add(AccountMeta(tokenMint, false, false))
            add(AccountMeta(destination, false, true))
            add(AccountMeta(owner, true, false))
        }
        val transactionData = encodeTransferCheckedTokenInstructionData(amount, decimals)

        return Program.createTransactionInstruction(programId.value, keys, transactionData)
    }

    @Suppress("MagicNumber")
    private fun encodeTransferCheckedTokenInstructionData(amount: Long, decimals: Byte): ByteArray? {
        val result = ByteBuffer.allocate(10).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(12.toByte())
            putLong(amount)
            put(decimals)
        }

        return result.array()
    }

    enum class ID(val value: PublicKey) {
        TOKEN(value = PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")),
        TOKEN_2022(value = PublicKey("TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb")),
    }

    fun isTokenProgram(address: String): Boolean {
        return TOKEN.value.toBase58() == address || TOKEN_2022.value.toBase58() == address
    }
}