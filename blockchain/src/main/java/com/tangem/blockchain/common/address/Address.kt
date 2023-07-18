package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

abstract class Address(
    open val value: String,
    open val type: AddressType,
    open val publicKey: Wallet.PublicKey,
)



