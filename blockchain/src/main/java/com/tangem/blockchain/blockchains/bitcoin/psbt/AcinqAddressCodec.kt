package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.psbt.PsbtAddressCodec
import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.utils.Either

internal class AcinqAddressCodec(private val blockchain: Blockchain) : PsbtAddressCodec {

    override fun scriptToAddress(scriptPubKey: ByteArray): String? {
        val chainHash = if (blockchain.isTestnet()) {
            Block.Testnet3GenesisBlock.hash
        } else {
            Block.LivenetGenesisBlock.hash
        }
        return when (val decoded = Bitcoin.addressFromPublicKeyScript(chainHash, scriptPubKey)) {
            is Either.Right -> decoded.value
            is Either.Left -> null
        }
    }
}