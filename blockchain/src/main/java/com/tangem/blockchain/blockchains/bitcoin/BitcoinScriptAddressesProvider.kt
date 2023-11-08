package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.BitcoinScriptAddress

interface BitcoinScriptAddressesProvider {

    fun makeAddresses(publicKey: Wallet.PublicKey, pairPublicKey: ByteArray) : List<BitcoinScriptAddress>

}
