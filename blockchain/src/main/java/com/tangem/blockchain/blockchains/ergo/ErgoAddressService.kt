package com.tangem.blockchain.blockchains.ergo

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.wallet.secrets.ExtendedPublicKey

class ErgoAddressService(private val isTestNet: Boolean) : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return Address.createEip3Address(
            0,
            if (isTestNet) NetworkType.TESTNET else NetworkType.MAINNET,
            ExtendedPublicKey(walletPublicKey, null, null)
        ).toString()
    }

    override fun validate(address: String): Boolean {
        return try {
            Address.create(address)
            true
        } catch (exception: Exception) {
            false
        }
    }
}