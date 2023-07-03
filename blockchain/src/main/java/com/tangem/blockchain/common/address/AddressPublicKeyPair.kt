package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

data class AddressPublicKeyPair(
    val value: String,
    val publicKey: Wallet.PublicKey,
    val type: AddressType
)