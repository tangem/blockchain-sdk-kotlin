package com.tangem.blockchain.blockchains.radiant

import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.script.ScriptBuilder

internal object RadiantAddressUtils {

    fun generateAddressScriptHash(walletAddress: String): String {
        val address = LegacyAddress.fromBase58(RadiantMainNetParams(), walletAddress)
        val p2pkhScript = ScriptBuilder.createOutputScript(address)
        val sha256Hash = Sha256Hash.hash(p2pkhScript.program)
        return sha256Hash.reversedArray().toHexString()
    }
}