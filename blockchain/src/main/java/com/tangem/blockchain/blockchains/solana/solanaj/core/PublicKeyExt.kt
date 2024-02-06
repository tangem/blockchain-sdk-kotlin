package com.tangem.blockchain.blockchains.solana.solanaj.core

import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgramId
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program

/**
 * Same as [org.p2p.solanaj.core.PublicKey.associatedTokenAddress] but with [tokenProgramId]
 * */
internal fun createAssociatedSolanaTokenAddress(
    account: PublicKey,
    mint: PublicKey,
    tokenProgramId: SolanaTokenProgramId,
): Result<PublicKey> = runCatching {
    val seeds = buildList {
        add(account.toByteArray())
        add(tokenProgramId.value.toByteArray())
        add(mint.toByteArray())
    }
    val foundProgram = PublicKey.findProgramAddress(seeds, Program.Id.splAssociatedTokenAccount)

    foundProgram.address
}
