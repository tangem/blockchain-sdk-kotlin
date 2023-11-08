package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import org.p2p.solanaj.core.PublicKey

/**
 * Created by Anton Zhilenkov on 21/01/2022.
 */
class SolanaAddressService : AddressService {

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        return PlainAddress(
            value = PublicKey(publicKey.blockchainKey).toBase58(),
            type = addressType,
            publicKey = publicKey
        )
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