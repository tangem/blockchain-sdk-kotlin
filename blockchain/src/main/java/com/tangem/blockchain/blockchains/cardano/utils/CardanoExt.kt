package com.tangem.blockchain.blockchains.cardano.utils

import com.tangem.blockchain.blockchains.cardano.CardanoTokenAddressConverter

internal fun String.matchesCardanoAsset(policyId: String, assetNameHex: String): Boolean {
    val assetFingerprint = CardanoTokenAddressConverter().convertToFingerprint(address = policyId + assetNameHex)

    return this == assetFingerprint
}