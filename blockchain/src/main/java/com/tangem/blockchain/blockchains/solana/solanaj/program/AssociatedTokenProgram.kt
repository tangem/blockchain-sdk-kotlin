package com.tangem.blockchain.blockchains.solana.solanaj.program

import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
[REDACTED_AUTHOR]
 */
class AssociatedTokenProgram : Program() {

    // not verified
    fun createATokenAccount(
        source: PublicKey,
        associatedAccountAddress: PublicKey,
        walletAddress: PublicKey,
        tokenMintAddress: PublicKey
    ): TransactionInstruction {
        val keys = mutableListOf<AccountMeta>()

        keys.add(AccountMeta(source, true, true))
        keys.add(AccountMeta(associatedAccountAddress, false, true))
        keys.add(AccountMeta(walletAddress, false, false))
        keys.add(AccountMeta(tokenMintAddress, false, false))
        keys.add(AccountMeta(Id.system, false, false))
        keys.add(AccountMeta(Id.token, false, false))
        keys.add(AccountMeta(Id.sysvarRent, false, false))

        val buffer = ByteBuffer.allocate(1)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(1.toByte())

        return createTransactionInstruction(Id.splAssociatedTokenAccount, keys, buffer.array())
    }
}