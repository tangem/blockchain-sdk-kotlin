package com.tangem.blockchain.blockchains.ergo

import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.extensions.calculateBlake2b
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toHexString
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

class ErgoAddressService(private val isTestnet: Boolean) : AddressService() {
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return fromSk(walletPublicKey).encodeBase58()
    }

    //add networkcheck
    override fun validate(address: String): Boolean {
        return try {
            val decoded = address.decodeBase58() ?: return false
            val size = decoded.size
            val script = decoded.sliceArray(0..size - 5)
            val checksum = decoded.sliceArray(size - 4 until size)
            script.calculateBlake2b(32).sliceArray(0..3).toHexString() == checksum.toHexString()
        }  catch (e: Exception) {
            false
        }
    }

    private fun fromSk(walletPublicKey: ByteArray): ByteArray{
        val curve = ECNamedCurveTable.getParameterSpec("secp256k1").g.multiply(BigInteger(1, walletPublicKey)).getEncoded(true)
        return fromPk(curve)
    }

    private fun fromPk(walletSecretKey: ByteArray): ByteArray{
        val prefixByte = Hex.decode(if (isTestnet) TESTNET_PREFIX else MAINNET_PREFIX)
        val checksum = prefixByte.plus(walletSecretKey).calculateBlake2b(32)
        return prefixByte.plus(walletSecretKey).plus(checksum).sliceArray(0..37)
    }

    companion object {
        private const val MAINNET_PREFIX = "01"
        private const val TESTNET_PREFIX = "11"
    }
}
