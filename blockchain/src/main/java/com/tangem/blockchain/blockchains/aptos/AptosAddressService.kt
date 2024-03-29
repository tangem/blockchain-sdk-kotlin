package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.address.TrustWalletAddressService
import com.tangem.common.card.EllipticCurve

/**
 * Address service implementation for [Blockchain.Aptos] based on [TrustWalletAddressService]
 *
 * @property isTestnet flag that indicates if current network is testnet
 *
 * @author Andrew Khokhlov on 26/03/2024
 */
internal class AptosAddressService(private val isTestnet: Boolean) : TrustWalletAddressService(
    blockchain = if (isTestnet) Blockchain.AptosTestnet else Blockchain.Aptos,
) {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val address = super.makeAddress(walletPublicKey, curve)

        return if (address.isStandardLength()) address else address.insertNonsignificantZero()
    }

    private fun String.isStandardLength(): Boolean {
        return removePrefix(HEX_PREFIX).length == APTOS_HEX_ADDRESS_LENGTH
    }

    private fun String.insertNonsignificantZero(): String {
        val addressWithoutPrefix = removePrefix(HEX_PREFIX)

        return buildString {
            append(HEX_PREFIX)

            repeat(APTOS_HEX_ADDRESS_LENGTH - addressWithoutPrefix.length) {
                append(NONSIGNIFICANT_ZERO)
            }

            append(addressWithoutPrefix)
        }
    }

    private companion object {
        const val APTOS_HEX_ADDRESS_LENGTH = 64
        const val NONSIGNIFICANT_ZERO = "0"
    }
}
