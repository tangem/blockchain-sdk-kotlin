package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

data class AddressPublicKeyPair(
    override val value: String,
    val publicKey: Wallet.PublicKey,
    override val type: AddressType
) : Address(value, type)