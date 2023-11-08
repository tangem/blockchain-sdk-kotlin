package com.tangem.blockchain.blockchains.chia

import Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.blockchains.chia.clvm.Program
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes

class ChiaAddressService(blockchain: Blockchain) : AddressService {

    private val humanReadablePart = when (blockchain) {
        Blockchain.Chia -> "xch"
        Blockchain.ChiaTestnet -> "txch"
        else -> throw IllegalStateException("$blockchain isn't supported")
    }

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve
    ): PlainAddress {
        val puzzle = getPuzzle(publicKey.blockchainKey)
        val puzzleHash = Program.deserialize(puzzle).hash()

        val address = Bech32.encode(
            humanReadablePart = humanReadablePart,
            dataIn = puzzleHash.toUByteArray()
        )

        return PlainAddress(
            value = address,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return try {
            val decoded = Bech32.decode(address)
            decoded.humanReadablePart == humanReadablePart
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        // curried and serialized signature.clsp (https://github.com/Chia-Network/chialisp-crash-course/blob/af620db2505db507b348d4f036dc4955fa81a004/signature.clsp)
        fun getPuzzle(walletPublicKey: ByteArray): ByteArray {
            return "ff02ffff01ff02ffff01ff04ffff04ff04ffff04ff05ffff04ffff02ff06ffff04ff02ffff04ff0bff80808080ff80808080ff0b80ffff04ffff01ff32ff02ffff03ffff07ff0580ffff01ff0bffff0102ffff02ff06ffff04ff02ffff04ff09ff80808080ffff02ff06ffff04ff02ffff04ff0dff8080808080ffff01ff0bffff0101ff058080ff0180ff018080ffff04ffff01b0"
                .hexToBytes() + walletPublicKey + "ff018080".hexToBytes()
        }

        fun getPuzzleHash(address: String): ByteArray {
            val dataBytes = Bech32.decode(address).data.toByteArray()
            return Crypto.convertBits(dataBytes, 0, dataBytes.size, 5, 8, false)
        }
    }
}
