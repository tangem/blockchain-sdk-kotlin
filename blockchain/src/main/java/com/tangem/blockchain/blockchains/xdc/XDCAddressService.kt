package com.tangem.blockchain.blockchains.xdc

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.common.card.EllipticCurve

internal class XDCAddressService : EthereumAddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val ethAddress = super.makeAddress(walletPublicKey, curve)

        return makeWithXdcPrefix(ethAddress)
    }

    override fun validate(address: String): Boolean {
        return super.validate(address.replace(XDC_PREFIX, ETH_PREFIX))
    }

    companion object {
        private const val ETH_PREFIX = "0x"
        private const val XDC_PREFIX = "xdc"

        fun makeWith0xPrefix(address: String): String {
            return if (address.startsWith(XDC_PREFIX)) {
                address.replace(XDC_PREFIX, ETH_PREFIX)
            } else {
                address
            }
        }

        fun makeWithXdcPrefix(address: String): String {
            return if (address.startsWith(ETH_PREFIX)) {
                address.replace(ETH_PREFIX, XDC_PREFIX)
            } else {
                address
            }
        }
    }
}