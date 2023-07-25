package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet
import org.bitcoinj.script.Script

data class BitcoinScriptAddress(
    val script: Script,
    override val value: String,
    override val type: AddressType = AddressType.Default,
    override val publicKey: Wallet.PublicKey
) : Address(value, type, publicKey)