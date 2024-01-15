package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.hedera.hashgraph.sdk.PublicKey
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve

class HederaAddressService(isTestnet: Boolean) : AddressService() {

    private val client = if (isTestnet) Client.forTestnet() else Client.forMainnet()

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        // TODO try to load from cache
        return ""
    }

    override fun validate(address: String): Boolean {
        return try {
            AccountId.fromString(address).validateChecksum(client) // won't fail if there is no checksum
            true
        } catch (e: Exception) {
            false
        }
    }
}
