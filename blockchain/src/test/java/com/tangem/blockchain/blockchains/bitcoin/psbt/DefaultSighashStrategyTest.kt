package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.PsbtHashComputer
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.extensions.Result
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DefaultSighashStrategyTest {

    @Test
    fun `sighash byte is SIGHASH_ALL`() {
        assertEquals(PsbtSighash.ALL, DefaultSighashStrategy.sighashByte)
    }

    @Test
    fun `delegates to PsbtHashComputer for a witness input`() {
        val psbt = PsbtTestFixtures.singleP2wpkhInputPsbt()
        val expected = (PsbtHashComputer.computeHashToSign(psbt, 0, PsbtSighash.ALL) as Result.Success).data
        val actual = (DefaultSighashStrategy.computeHashToSign(psbt, 0, PsbtSighash.ALL) as Result.Success).data
        assertArrayEquals(expected, actual)
    }

    @Test
    fun `honors a non-default per-input sighash type instead of always using SIGHASH_ALL`() {
        val psbt = PsbtTestFixtures.singleP2wpkhInputPsbt()
        val expectedDefault = (PsbtHashComputer.computeHashToSign(psbt, 0, PsbtSighash.ALL) as Result.Success).data
        val expectedNonDefault = (PsbtHashComputer.computeHashToSign(psbt, 0, 0x83) as Result.Success).data
        val actualNonDefault = (DefaultSighashStrategy.computeHashToSign(psbt, 0, 0x83) as Result.Success).data

        assertArrayEquals(expectedNonDefault, actualNonDefault)
        assertFalse(expectedDefault.contentEquals(actualNonDefault))
    }
}