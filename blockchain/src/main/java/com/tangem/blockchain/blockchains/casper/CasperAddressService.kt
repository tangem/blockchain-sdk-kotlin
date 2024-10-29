package com.tangem.blockchain.blockchains.casper

import com.tangem.blockchain.blockchains.casper.utils.CasperAddressUtils.checksum
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.extensions.isSameCase
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes

internal class CasperAddressService : AddressService() {
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val prefix = CasperConstants.getAddressPrefix(curve!!)
        val bytes = prefix.hexToBytes() + walletPublicKey
        return bytes.checksum()
    }

    override fun validate(address: String): Boolean {
        val isCorrectEd25519Address = address.length == CasperConstants.ED25519_LENGTH &&
            address.startsWith(CasperConstants.ED25519_PREFIX)
        val isCorrectSecp256k1Address = address.length == CasperConstants.SECP256K1_LENGTH &&
            address.startsWith(CasperConstants.SECP256K1_PREFIX)

        if (!isCorrectEd25519Address && !isCorrectSecp256k1Address) {
            return false
        }

        // don't check checksum if it's not mixed case
        if (address.isSameCase()) {
            return true
        }

        return address == address.hexToBytes().checksum()
    }
}