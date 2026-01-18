package com.tangem.blockchain.blockchains.chia

import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.blockchains.chia.clvm.Program
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Bech32

class ChiaAddressService(blockchain: Blockchain) : AddressService() {

    private val humanReadablePart = when (blockchain) {
        Blockchain.Chia -> HRP_MAINNET
        Blockchain.ChiaTestnet -> HRP_TESTNET
        else -> error("$blockchain isn't supported")
    }

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val puzzle = getPuzzle(walletPublicKey)
        val puzzleHash = Program.deserialize(puzzle).hash()
        val fiveBitData = Crypto.convertBits(puzzleHash, 0, puzzleHash.size, FROM_BITS, TO_BITS, true)
        return Bech32.encode(Bech32.Encoding.BECH32M, humanReadablePart, fiveBitData)
    }

    override fun validate(address: String): Boolean {
        return try {
            val decoded = Bech32.decode(address)
            decoded.hrp == humanReadablePart && decoded.encoding == Bech32.Encoding.BECH32M
        } catch (e: AddressFormatException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    companion object {
        private const val HRP_MAINNET = "xch"
        private const val HRP_TESTNET = "txch"
        private const val FROM_BITS = 8
        private const val TO_BITS = 5

        // curried and serialized signature.clsp (https://github.com/Chia-Network/chialisp-crash-course/blob/af620db2505db507b348d4f036dc4955fa81a004/signature.clsp)
        fun getPuzzle(walletPublicKey: ByteArray): ByteArray {
            return (
                "ff02ffff01ff02ffff01ff04ffff04ff04ffff04ff05ffff04ffff02ff06ffff04ff02ffff04ff0bff80808080ff8080808" +
                    "0ff0b80ffff04ffff01ff32ff02ffff03ffff07ff0580ffff01ff0bffff0102ffff02ff06ffff04ff02ffff04ff09ff" +
                    "80808080ffff02ff06ffff04ff02ffff04ff0dff8080808080ffff01ff0bffff0101ff058080ff0180ff018080ffff0" +
                    "4ffff01b0"
                ).hexToBytes() + walletPublicKey + "ff018080".hexToBytes()
        }

        fun getPuzzleHash(address: String): ByteArray {
            val decoded = Bech32.decode(address)
            return Crypto.convertBits(decoded.data, 0, decoded.data.size, TO_BITS, FROM_BITS, false)
        }
    }
}