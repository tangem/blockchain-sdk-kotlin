package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

data class PlainAddress(
    override val value: String,
    override val type: AddressType,
    override val publicKey: Wallet.PublicKey,
) : Address(value, type, publicKey)