package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import org.p2p.solanaj.core.PublicKey

/**
[REDACTED_AUTHOR]
 */
class SolanaAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return PublicKey(walletPublicKey).toBase58()
    }

    override fun validate(address: String): Boolean {
        return try {
            PublicKey(address)
            true
        } catch (ex: Exception) {
            false
        }
    }
}