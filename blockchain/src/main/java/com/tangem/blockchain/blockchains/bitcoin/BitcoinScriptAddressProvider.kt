package com.tangem.blockchain.blockchains.bitcoin

import org.bitcoinj.script.Script

interface BitcoinScriptAddressProvider {

    fun makeScriptAddress(script: Script) : String

}

