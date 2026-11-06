package com.tangem.blockchain.blockchains.solana.nft

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

internal class MplCoreProgramTest {

    private val asset = PublicKey("3xYi4gjZ8HKp8N2yEHiYKjVrTKX3K9KcC33qYxWzbDFZ")
    private val collection = PublicKey("CCryptUfeFSZ3Fgc9FLeKrhLVAP67FSqi1GuVoj9CRac")
    private val payer = PublicKey("CUfZZHt9nNsSnQUv5jhGGLbRRtudpWvzoSwbJQf7T9PW")
    private val newOwner = PublicKey("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo")

    @Test
    fun `transfer passes seven accounts and the TransferV1 data`() {
        val instruction = MplCoreProgram.transferV1(
            asset = asset,
            collection = collection,
            payer = payer,
            newOwner = newOwner,
        )
        val program = MplCoreProgram.programId

        assertThat(instruction.programId).isEqualTo(program)
        assertThat(instruction.data.toList()).containsExactly(0x0E.toByte(), 0x00.toByte()).inOrder()
        assertThat(instruction.keys.map { Triple(it.publicKey, it.isSigner, it.isWritable) }).containsExactly(
            Triple(asset, false, true),
            Triple(collection, false, false),
            Triple(payer, true, true),
            Triple(program, false, false),
            Triple(newOwner, false, false),
            Triple(program, false, false),
            Triple(program, false, false),
        ).inOrder()
    }

    @Test
    fun `asset without collection passes the program in the collection slot`() {
        val instruction = MplCoreProgram.transferV1(
            asset = asset,
            collection = null,
            payer = payer,
            newOwner = newOwner,
        )

        assertThat(instruction.keys[1].publicKey).isEqualTo(MplCoreProgram.programId)
    }
}