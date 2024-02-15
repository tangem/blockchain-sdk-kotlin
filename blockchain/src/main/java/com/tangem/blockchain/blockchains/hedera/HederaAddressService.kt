package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve

internal class HederaAddressService(isTestnet: Boolean) : AddressService() {

    private val client = if (isTestnet) Client.forTestnet() else Client.forMainnet()

    // Address for Hedera generates with backend, and will be received in update wallet flow
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String = ""

    override fun validate(address: String): Boolean {
        // won't fail if there is no checksum
        return runCatching { AccountId.fromString(address).validateChecksum(client) }
            .fold(onSuccess = { true }, onFailure = { false })
    }
}