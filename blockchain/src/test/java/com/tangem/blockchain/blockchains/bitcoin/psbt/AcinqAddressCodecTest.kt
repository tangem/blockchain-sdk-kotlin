package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcinqAddressCodecTest {

    // Well-known Bitcoin Wiki example pubkey; hash160 of it maps to "1PMycacnJaSqwwJqjawXBErnLsZ7RkXUAs".
    private val pubKeyHash = Crypto.hash160(
        ByteVector("0250863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b2352".hexToBytes()).toByteArray(),
    )

    @Test
    fun `decodes P2PKH script to mainnet base58 address`() {
        val codec = AcinqAddressCodec(Blockchain.Bitcoin)
        val script = Script.write(Script.pay2pkh(pubKeyHash))
        val address = codec.scriptToAddress(script)
        assertEquals("1PMycacnJaSqwwJqjawXBErnLsZ7RkXUAs", address)
    }

    @Test
    fun `returns null for OP_RETURN script`() {
        val codec = AcinqAddressCodec(Blockchain.Bitcoin)
        val opReturn = byteArrayOf(0x6a, 0x02, 0x01, 0x02)
        assertNull(codec.scriptToAddress(opReturn))
    }
}