package com.tangem.blockchain.blockchains.solana.solanaj.program

import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program

internal enum class SolanaTokenProgramId(val value: PublicKey) {
    TOKEN(value = Program.Id.token),
    TOKEN_2022(value = PublicKey("TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb")),
}
