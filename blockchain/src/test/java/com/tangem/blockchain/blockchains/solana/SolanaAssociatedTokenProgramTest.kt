package com.tangem.blockchain.blockchains.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaAssociatedTokenProgram
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

internal class SolanaAssociatedTokenProgramTest {

    @Test
    fun `payload is the idempotent variant`() {
        // The only reason this object exists: solanaj's createAssociatedTokenAccountInstruction sends an empty
        // payload, which is the 0x00 variant, and that one fails once the account exists. Replacing this with the
        // library version would look like a simplification and would break a repeated onboarding.
        val instruction = SolanaAssociatedTokenProgram.createIdempotentInstruction(
            payer = PublicKey("8Yv9Jz4z7BUHP68dz8E8m3tMe6NKgpMUKn8KVqhPKuwj"),
            associatedAccount = PublicKey("6atEYZQxh5sYXjnwS5WfVvnh6p8wUXibc2xcD2Gftfqs"),
            owner = PublicKey("aBRHcE97NY1MjVqGbc2LX2cApfPg9r5h5T1KNZS93oA"),
            mint = PublicKey("Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr"),
            tokenProgramId = SolanaTokenProgram.ID.TOKEN,
        )

        assertThat(instruction.data).isEqualTo(byteArrayOf(0x01))
    }
}