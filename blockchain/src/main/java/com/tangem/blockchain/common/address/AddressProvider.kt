package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet

interface AddressProvider {
    fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): AddressPublicKeyPair
}
