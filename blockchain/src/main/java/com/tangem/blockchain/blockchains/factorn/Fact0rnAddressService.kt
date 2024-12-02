package com.tangem.blockchain.blockchains.factorn

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder

internal class Fact0rnAddressService : AddressService() {

    private val bitcoinAddressService = BitcoinAddressService(Blockchain.Fact0rn)

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return bitcoinAddressService.makeSegwitAddress(walletPublicKey).value
    }

    override fun validate(address: String): Boolean {
        return bitcoinAddressService.validateSegwitAddress(address)
    }

    companion object {

        internal fun addressToScript(address: String): Script =
            ScriptBuilder.createOutputScript(SegwitAddress.fromBech32(Fact0rnMainNetParams(), address))

        internal fun addressToScriptHash(address: String): String {
            val p2pkhScript = addressToScript(address)
            val sha256Hash = Sha256Hash.hash(p2pkhScript.program)
            return sha256Hash.reversedArray().toHexString()
        }
    }
}