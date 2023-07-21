package com.tangem.blockchain.blockchains.rsk

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.kethereum.crypto.toAddress
import org.kethereum.erc55.isValid
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import java.util.*

class RskAddressService : AddressService {

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): PlainAddress {
        val checksumAddress = PublicKey(
            publicKey.blockchainKey.toDecompressedPublicKey().sliceArray(1..64)
        ).toAddress().withChecksum().hex

        return PlainAddress(
            value = checksumAddress,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean = Address(address).hasValidChecksumOrNoChecksum()

    private fun Address.withChecksum(): Address { // it's like ERC55 but with chainId
        return ("30$hex").toLowerCase(Locale.ROOT).toByteArray().keccak().toHexString()
            .let { hexHash ->
                Address(cleanHex.mapIndexed { index, hexChar ->
                    when {
                        hexChar in '0'..'9' -> hexChar
                        hexHash[index] in '0'..'7' -> hexChar.toLowerCase()
                        else -> hexChar.toUpperCase()
                    }
                }.joinToString(""))
            }
    }

    private fun Address.hasValidChecksumOrNoChecksum(): Boolean {
        return isValid() &&
            (withChecksum().hex == hex ||
                cleanHex.toLowerCase() == cleanHex ||
                cleanHex.toUpperCase() == cleanHex)
    }
}