package com.tangem.blockchain.blockchains.bitcoincash.psbt

import android.util.Base64
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.PsbtSerializer
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.toHexString
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BitcoinCashSighashStrategy].
 *
 * The oracle hash below was computed by an independent BIP143+FORKID implementation
 * (NOT bitcoinj), so a match also proves the acinq [fr.acinq.bitcoin.psbt.Psbt] -> bitcoinj
 * [com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransaction] bridge (amounts,
 * scripts, txid endianness) is correct.
 *
 * @see com.tangem.blockchain.blockchains.bitcoincash.psbt.BitcoinCashSighashStrategy PSBT source docs
 */
internal class BitcoinCashSighashStrategyTest {

    // Real BCH swap PSBT (Express QA swap response, txDetailsJson.txData).
    // See docs/superpowers/specs/[REDACTED_TASK_KEY]-bch-spike-notes.md for the decoded facts.
    private val realBchPsbtBase64 = "cHNidP8BAHcCAAAAAbjn+ZqPMkrnS9W31VCpc6Fv6qGPNLJmG0MMc2kuq401AgAAAAD/////Am3vaQMAAAAAGXapFKpUGcV2GjfcK1L1dxLg0WAlGPCPiKycdQBjAwAAABl2qRReo8//nGk3TZTpWwg4rqeFSftUGoisAAAAAAABAP2YAQEAAAACN/awtEDlWY0arLt1HxCK6djWgA4ZPIYo92MB2/R+VgYBAAAAa0gwRQIhAOj0HCt92+XlZTBf8kRcAM8sDoO1x2IsjzUSXR+CUs+GAiBOyhp2mdFxNIsWIdVB5WkIyrsqAcPfF8uEbJaz3vAg4UEhAlDxLZGUTsZ8XJEJPm6GYNByd1gU9833cDxfyjUdZkVu/////13x64t7ifOo59dFwEVVBAPxsY/DjZtpCY2Nh7Li4Ii5AQAAAGtIMEUCIQCfK2OVyR0lzEUDI44oZM0so1+w7kZy5E3ioapEKQeydgIgKQOteHrYyTaQSlGiAxh37ThZuyPPviwtyE2VMIPgH+RBIQJQ8S2RlE7GfFyRCT5uhmDQcndYFPfN93A8X8o1HWZFbv////8D4MN5AAAAAAAZdqkU3TpcdbgMWZZcpxfVBakbUKT09+WIrGBImAAAAAAAGXapFDAqIo3Qq4HVxVDRhfQQr75OIXFLiKzrZWpmAwAAABl2qRReo8//nGk3TZTpWwg4rqeFSftUGoisAAAAAAEDBEEAAAAAAAA="

    // Unsigned tx (single input, 2 outputs) reconstructed from the global.tx of realBchPsbtBase64.
    private val unsignedTxHex = "0200000001b8e7f99a8f324ae74bd5b7d550a973a16feaa18f34b2661b430c73692eab8d3" +
        "50200000000ffffffff026def6903000000001976a914aa5419c5761a37dc2b52f57712e0d1602518f08f88ac9c7500630300" +
        "00001976a9145ea3cfff9c69374d94e95b0838aea78549fb541a88ac00000000"

    // Independent oracle: computed by a separate BIP143+forkid implementation (NOT bitcoinj).
    private val expectedOracleHashHex = "05636ea194312008aec2a35904bf7af9feb67f2dff45d2ec7bc16ea30a94beb1"

    @Before
    fun setup() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `sighash byte is FORKID`() {
        assertEquals(PsbtSighash.ALL_FORKID, BitcoinCashSighashStrategy(Blockchain.BitcoinCash).sighashByte)
    }

    @Test
    fun `hash matches independent BIP143-forkid oracle`() {
        // Given
        val psbt = (PsbtSerializer.parsePsbt(realBchPsbtBase64) as Result.Success).data

        // Sanity check: the reconstructed unsigned tx bytes (as parsed by acinq) must equal the
        // known-good unsigned tx hex from the spike notes. If this fails, the fixture itself is wrong,
        // not the strategy.
        assertEquals(
            unsignedTxHex,
            fr.acinq.bitcoin.Transaction.write(psbt.global.tx).toHexString().lowercase(),
        )

        // When
        val result = BitcoinCashSighashStrategy(Blockchain.BitcoinCash).computeHashToSign(
            psbt = psbt,
            inputIndex = 0,
            sighashType = PsbtSighash.ALL_FORKID,
        )

        // Then
        val actual = (result as Result.Success).data
        assertEquals(expectedOracleHashHex, actual.toHexString().lowercase())
    }

    @Test
    fun `computeHashToSign rejects a sighashType other than FORKID`() {
        // Given
        val psbt = (PsbtSerializer.parsePsbt(realBchPsbtBase64) as Result.Success).data

        // When — plain SIGHASH_ALL (0x01), not BCH's FORKID-tagged 0x41
        val result = BitcoinCashSighashStrategy(Blockchain.BitcoinCash).computeHashToSign(
            psbt = psbt,
            inputIndex = 0,
            sighashType = PsbtSighash.ALL,
        )

        // Then
        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is BlockchainSdkError.CustomError)
        assertTrue((error as BlockchainSdkError.CustomError).customMessage.contains("FORKID"))
    }
}