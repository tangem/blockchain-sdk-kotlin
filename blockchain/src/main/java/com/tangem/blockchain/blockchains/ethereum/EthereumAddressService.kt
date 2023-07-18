package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.crypto.toAddress
import org.kethereum.erc55.hasValidERC55ChecksumOrNoChecksum
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.model.Address
import org.kethereum.model.PublicKey

class EthereumAddressService : AddressService {

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): PlainAddress {
        val address = PublicKey(
            publicKey.blockchainKey.toDecompressedPublicKey().sliceArray(1..64)
        ).toAddress().withERC55Checksum().hex

        return PlainAddress(
            value = address,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean = Address(address).hasValidERC55ChecksumOrNoChecksum()

}