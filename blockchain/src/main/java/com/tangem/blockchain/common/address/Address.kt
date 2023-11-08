package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

/**
 * Address entity
 * Has two inheritors:
 *
 * usual address
 * @see com.tangem.blockchain.common.address.PlainAddress
 *
 * bitcoin address with extra info (script property)
 * @see com.tangem.blockchain.common.address.BitcoinScriptAddress
 *
 * @property value Address value
 * @property type Address type, default or legacy
 * @property publicKey Public key that was used for address generating
 */
abstract class Address(
    open val value: String,
    open val type: AddressType,
    open val publicKey: Wallet.PublicKey,
)



