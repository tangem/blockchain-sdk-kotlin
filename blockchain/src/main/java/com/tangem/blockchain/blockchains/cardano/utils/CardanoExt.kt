package com.tangem.blockchain.blockchains.cardano.utils

import com.tangem.blockchain.blockchains.cardano.CardanoTokenAddressConverter

internal fun String.isCardanoAsset(policyId: String, assetName: String): Boolean {
    val assetFingerprint = CardanoTokenAddressConverter().convertToFingerprint(address = policyId + assetName)

    return this == assetFingerprint || this.startsWith(prefix = policyId)
}