package com.tangem.blockchain.blockchains.bitcoincash.psbt

import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.CashAddr
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.Script
import org.bitcoinj.script.ScriptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BitcoinCashAddressCodecTest {

    // Well-known Bitcoin Wiki example pubkey; see [REDACTED_TASK_KEY] memory for the verified P2PKH vector.
    private val publicKey =
        "0250863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b2352".hexToBytes()

    @Test
    fun `P2PKH script decodes to the wallet CashAddr`() {
        val service = BitcoinCashAddressService(Blockchain.BitcoinCash)
        val expected = service.makeAddress(publicKey) // bitcoincash:q...
        val hash = Crypto.hash160(ByteVector(publicKey).toByteArray())
        val script = Script.write(Script.pay2pkh(hash))
        val codec = BitcoinCashAddressCodec(Blockchain.BitcoinCash)
        assertEquals(expected, codec.scriptToAddress(script))
    }

    @Test
    fun `P2SH script decodes to a CashAddr`() {
        val scriptHash = "1234567890abcdef1234567890abcdef12345678".hexToBytes()
        val script = ScriptBuilder.createP2SHOutputScript(scriptHash).program
        val expected = CashAddr(false).toCashAddress(BitcoinCashAddressType.P2SH, scriptHash)
        val codec = BitcoinCashAddressCodec(Blockchain.BitcoinCash)
        assertEquals(expected, codec.scriptToAddress(script))
    }

    @Test
    fun `returns null for OP_RETURN`() {
        val codec = BitcoinCashAddressCodec(Blockchain.BitcoinCash)
        assertNull(codec.scriptToAddress(byteArrayOf(0x6a, 0x02, 0x01, 0x02)))
    }
}