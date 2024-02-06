package com.tangem.blockchain.blockchains.xdc

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve

internal class XDCAddressService : EthereumAddressService() {

    override fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve?): Set<Address> {
        return setOf(
            Address(makeAddressWithXdcPrefix(walletPublicKey, curve), AddressType.Default),
            Address(makeAddressWith0xPrefix(walletPublicKey, curve), AddressType.Legacy),
        )
    }

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return makeAddressWithXdcPrefix(walletPublicKey, curve)
    }

    override fun validate(address: String): Boolean {
        return super.validate(address.replace(XDC_PREFIX, ETH_PREFIX))
    }

    private fun makeAddressWith0xPrefix(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return super.makeAddress(walletPublicKey, curve)
    }

    private fun makeAddressWithXdcPrefix(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val ethAddress = makeAddressWith0xPrefix(walletPublicKey, curve)

        return formatWithXdcPrefix(ethAddress)
    }

    companion object {
        private const val ETH_PREFIX = "0x"
        private const val XDC_PREFIX = "xdc"

        fun formatWith0xPrefix(address: String): String {
            return if (address.startsWith(XDC_PREFIX)) {
                address.replace(XDC_PREFIX, ETH_PREFIX)
            } else {
                address
            }
        }

        fun formatWithXdcPrefix(address: String): String {
            return if (address.startsWith(ETH_PREFIX)) {
                address.replace(ETH_PREFIX, XDC_PREFIX)
            } else {
                address
            }
        }
    }
}
