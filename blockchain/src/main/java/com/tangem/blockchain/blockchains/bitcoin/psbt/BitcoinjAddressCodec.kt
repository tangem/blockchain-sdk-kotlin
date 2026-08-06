package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.blockchains.dash.DashMainNetParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.psbt.PsbtAddressCodec
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptPattern
import org.libdohj.params.DogecoinMainNetParams
import org.libdohj.params.LitecoinMainNetParams

internal class BitcoinjAddressCodec(blockchain: Blockchain) : PsbtAddressCodec {

    private val params: NetworkParameters = when (blockchain) {
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Dogecoin -> DogecoinMainNetParams()
        Blockchain.Dash -> DashMainNetParams()
        else -> error("BitcoinjAddressCodec does not support $blockchain")
    }

    override fun scriptToAddress(scriptPubKey: ByteArray): String? {
        return try {
            val script = Script(scriptPubKey)
            when {
                ScriptPattern.isP2PKH(script) ->
                    LegacyAddress.fromPubKeyHash(params, ScriptPattern.extractHashFromP2PKH(script)).toBase58()
                ScriptPattern.isP2SH(script) ->
                    LegacyAddress.fromScriptHash(params, ScriptPattern.extractHashFromP2SH(script)).toBase58()
                ScriptPattern.isP2WPKH(script) || ScriptPattern.isP2WH(script) ->
                    SegwitAddress.fromHash(params, ScriptPattern.extractHashFromP2WH(script)).toBech32()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}