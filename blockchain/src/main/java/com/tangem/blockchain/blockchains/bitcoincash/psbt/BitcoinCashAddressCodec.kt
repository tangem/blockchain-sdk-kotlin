package com.tangem.blockchain.blockchains.bitcoincash.psbt

import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.CashAddr
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.psbt.PsbtAddressCodec
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptPattern

internal class BitcoinCashAddressCodec(blockchain: Blockchain) : PsbtAddressCodec {

    private val cashAddr = CashAddr(blockchain == Blockchain.BitcoinCashTestnet)

    override fun scriptToAddress(scriptPubKey: ByteArray): String? {
        return try {
            val script = Script(scriptPubKey)
            when {
                ScriptPattern.isP2PKH(script) ->
                    cashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, ScriptPattern.extractHashFromP2PKH(script))
                ScriptPattern.isP2SH(script) ->
                    cashAddr.toCashAddress(BitcoinCashAddressType.P2SH, ScriptPattern.extractHashFromP2SH(script))
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}