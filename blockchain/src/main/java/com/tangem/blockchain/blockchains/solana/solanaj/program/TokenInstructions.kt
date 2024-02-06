package com.tangem.blockchain.blockchains.solana.solanaj.program

import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Same as [org.p2p.solanaj.programs.TokenProgram.transferChecked] but with [programId]
 * */
@Suppress("LongParameterList")
internal fun createSolanaTransferCheckedInstruction(
    source: PublicKey?,
    destination: PublicKey?,
    amount: Long,
    decimals: Byte,
    owner: PublicKey?,
    tokenMint: PublicKey?,
    programId: SolanaTokenProgramId,
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
