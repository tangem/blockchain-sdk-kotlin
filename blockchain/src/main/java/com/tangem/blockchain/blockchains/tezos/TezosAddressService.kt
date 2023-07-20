package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.Base58
import org.spongycastle.jcajce.provider.digest.Blake2b

class TezosAddressService(private val ellipticCurve: EllipticCurve) : AddressService {

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): PlainAddress {
        val publicKeyHash = Blake2b.Blake2b160().digest(publicKey.blockchainKey.toCompressedPublicKey())

        val prefix = TezosConstants.getAddressPrefix(ellipticCurve)
        val prefixedHash = prefix.hexToBytes() + publicKeyHash
        val checksum = prefixedHash.calculateTezosChecksum()

        return PlainAddress(
            value = Base58.encode(prefixedHash + checksum),
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return try {
            val prefixedHashWithChecksum = Base58.decode(address)
            if (prefixedHashWithChecksum == null || prefixedHashWithChecksum.size != 27) return false

            val prefixedHash = prefixedHashWithChecksum.copyOf(23)
            val checksum = prefixedHashWithChecksum.copyOfRange(23, 27)

            val calculatedChecksum = prefixedHash.calculateTezosChecksum()

            calculatedChecksum.contentEquals(checksum)
        } catch (exception: Exception) {
            false
        }
    }

    companion object {
        fun ByteArray.calculateTezosChecksum() =
                this.calculateSha256().calculateSha256().copyOfRange(0, 4)
    }
}