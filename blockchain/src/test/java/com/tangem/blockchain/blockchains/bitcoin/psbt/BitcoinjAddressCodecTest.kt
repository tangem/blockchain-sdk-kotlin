package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.dash.DashMainNetParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.script.ScriptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.libdohj.params.DogecoinMainNetParams
import org.libdohj.params.LitecoinMainNetParams

class BitcoinjAddressCodecTest {

    private val publicKey =
        "0250863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b2352".hexToBytes()

    // Litecoin/Dogecoin/Dash NetworkParameters are never registered with bitcoinj's global
    // Networks registry, so LegacyAddress.fromBase58(null, ...) / SegwitAddress.fromBech32(null, ...)
    // cannot auto-detect the network (throws AddressFormatException.InvalidPrefix). Pass explicit
    // params instead - deviation from the brief's test snippet, which used `null`.
    private fun paramsFor(blockchain: Blockchain): NetworkParameters = when (blockchain) {
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Dogecoin -> DogecoinMainNetParams()
        Blockchain.Dash -> DashMainNetParams()
        else -> error("Unsupported blockchain: $blockchain")
    }

    private fun p2pkhScript(blockchain: Blockchain): ByteArray {
        val legacy = BitcoinAddressService(blockchain).makeLegacyAddress(publicKey).value
        return ScriptBuilder.createOutputScript(
            LegacyAddress.fromBase58(paramsFor(blockchain), legacy),
        ).program
    }

    @Test
    fun `Litecoin P2PKH decodes to the wallet legacy address`() {
        val service = BitcoinAddressService(Blockchain.Litecoin)
        val expected = service.makeLegacyAddress(publicKey).value
        val codec = BitcoinjAddressCodec(Blockchain.Litecoin)
        assertEquals(expected, codec.scriptToAddress(p2pkhScript(Blockchain.Litecoin)))
    }

    @Test
    fun `Litecoin P2WPKH decodes to the wallet segwit address`() {
        val service = BitcoinAddressService(Blockchain.Litecoin)
        val expected = service.makeSegwitAddress(publicKey).value // ltc1...
        val segwitScript = ScriptBuilder.createOutputScript(
            SegwitAddress.fromBech32(paramsFor(Blockchain.Litecoin), expected),
        ).program
        val codec = BitcoinjAddressCodec(Blockchain.Litecoin)
        assertEquals(expected, codec.scriptToAddress(segwitScript))
    }

    @Test
    fun `Dogecoin P2PKH decodes to the wallet legacy address`() {
        val service = BitcoinAddressService(Blockchain.Dogecoin)
        val expected = service.makeLegacyAddress(publicKey).value
        val codec = BitcoinjAddressCodec(Blockchain.Dogecoin)
        assertEquals(expected, codec.scriptToAddress(p2pkhScript(Blockchain.Dogecoin)))
    }

    @Test
    fun `Dash P2PKH decodes to the wallet legacy address`() {
        val service = BitcoinAddressService(Blockchain.Dash)
        val expected = service.makeLegacyAddress(publicKey).value
        val codec = BitcoinjAddressCodec(Blockchain.Dash)
        assertEquals(expected, codec.scriptToAddress(p2pkhScript(Blockchain.Dash)))
    }

    @Test
    fun `returns null for OP_RETURN`() {
        val codec = BitcoinjAddressCodec(Blockchain.Litecoin)
        assertNull(codec.scriptToAddress(byteArrayOf(0x6a, 0x02, 0x01, 0x02)))
    }
}