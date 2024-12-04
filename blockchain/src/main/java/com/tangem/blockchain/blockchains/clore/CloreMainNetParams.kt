package com.tangem.blockchain.blockchains.clore

import org.bitcoinj.core.Block
import org.bitcoinj.core.Utils
import org.bitcoinj.params.AbstractBitcoinNetParams

@Suppress("MagicNumber")
internal class CloreMainNetParams : AbstractBitcoinNetParams() {
    init {
        interval = INTERVAL
        targetTimespan = TARGET_TIMESPAN

        // 00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL)
        addressHeader = 23
        p2shHeader = 122
        port = 8788
        bip32HeaderP2PKHpub = 0x0488b21e // The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ade4 // The 4 byte header that serializes in base58 to "xprv"
        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED
        majorityWindow = MAINNET_MAJORITY_WINDOW
        id = ID_MAINNET
        dnsSeeds = arrayOf(
            "seed.clore.ai",
            "seed1.clore.ai",
            "seed2.clore.ai",
        )
    }

    override fun getPaymentProtocolId(): String {
        return PAYMENT_PROTOCOL_ID_MAINNET
    }

    override fun getGenesisBlock(): Block {
        return Block.createGenesis(this) // Stub genesis block
    }

    private companion object {
        private const val MAINNET_MAJORITY_WINDOW = 1000
        private const val MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950
        private const val MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750
    }
}