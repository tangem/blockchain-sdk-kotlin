package com.tangem.blockchain.blockchains.solana.solanaj.program

import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/** SPL Memo v2. Without accounts the memo carries no signer check, it is only a UTF-8 string in the transaction. */
internal object SolanaMemoProgram {

    val programId = PublicKey("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")

    fun createMemoInstruction(memo: String): TransactionInstruction = Program.createTransactionInstruction(
        programId,
        emptyList(),
        memo.toByteArray(Charsets.UTF_8),
    )
}