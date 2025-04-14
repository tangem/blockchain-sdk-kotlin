package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.Base58
import org.spongycastle.jcajce.provider.digest.Blake2b

@Suppress("MagicNumber")
class TezosAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKeyHash = Blake2b.Blake2b160().digest(walletPublicKey.toCompressedPublicKey())

        val prefix = TezosConstants.getAddressPrefix(curve!!)
        val prefixedHash = prefix.hexToBytes() + publicKeyHash
        val checksum = prefixedHash.calculateTezosChecksum()

        return Base58.encode(prefixedHash + checksum)
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
        fun ByteArray.calculateTezosChecksum() = this.calculateSha256().calculateSha256().copyOfRange(0, 4)
    }
}
