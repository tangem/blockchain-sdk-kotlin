package com.tangem.blockchain.blockchains.radiant

import org.bitcoinj.core.Block
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Utils
import org.bitcoinj.params.AbstractBitcoinNetParams

@Suppress("MagicNumber")
internal class RadiantMainNetParams : AbstractBitcoinNetParams() {

    init {
        addressHeader = 0x00
        dumpedPrivateKeyHeader = 0x80
        p2shHeader = 0x05

        bip32HeaderP2PKHpub = 0x0488b21e // The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ade4 // The 4 byte header that serializes in base58 to "xprv"
        packetMagic = 0xe3e1f3e8
        port = 8333

        interval = INTERVAL
        targetTimespan = TARGET_TIMESPAN

        // 00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL)
        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED
        majorityWindow = MAINNET_MAJORITY_WINDOW
        id = ID_MAINNET
        dnsSeeds = arrayOf(
            "seed.bitcoinsv.org",
            "seed.bitcoinunlimited.info",
        )
    }

    override fun getPaymentProtocolId(): String = NetworkParameters.PAYMENT_PROTOCOL_ID_MAINNET
    override fun getGenesisBlock(): Block {
        return Block.createGenesis(this) // Stub genesis block
    }

    private companion object {
        const val MAINNET_MAJORITY_WINDOW = 1000
        const val MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950
        const val MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750
    }
}