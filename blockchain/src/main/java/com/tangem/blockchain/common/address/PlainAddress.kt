package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

data class PlainAddress(
    override val value: String,
    override val type: AddressType = AddressType.Default, // TODO refactoring
    override val publicKey: Wallet.PublicKey,
) : Address(value, type, publicKey)