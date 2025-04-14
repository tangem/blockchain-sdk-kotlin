package com.tangem.blockchain.blockchains.rsk

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.kethereum.crypto.toAddress
import org.kethereum.erc55.isValid
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import java.util.Locale

class RskAddressService : AddressService() {

    @Suppress("MagicNumber")
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String = PublicKey(
        walletPublicKey.toDecompressedPublicKey().sliceArray(1..64),
    ).toAddress().withChecksum().hex

    override fun validate(address: String): Boolean = Address(address).hasValidChecksumOrNoChecksum()

    private fun Address.withChecksum(): Address { // it's like ERC55 but with chainId
        return "30$hex".lowercase(Locale.ROOT).toByteArray().keccak().toHexString()
            .let { hexHash ->
                Address(
                    cleanHex.mapIndexed { index, hexChar ->
                        when {
                            hexChar in '0'..'9' -> hexChar
                            hexHash[index] in '0'..'7' -> hexChar.lowercaseChar()
                            else -> hexChar.uppercaseChar()
                        }
                    }.joinToString(""),
                )
            }
    }

    private fun Address.hasValidChecksumOrNoChecksum(): Boolean {
        return isValid() &&
            (
                withChecksum().hex == hex ||
                    cleanHex.lowercase(Locale.getDefault()) == cleanHex ||
                    cleanHex.uppercase(Locale.getDefault()) == cleanHex
                )
    }
}
