package com.tangem.blockchain.blockchains.ravencoin

import org.bitcoinj.core.Utils
import org.bitcoinj.params.AbstractBitcoinNetParams

class RavencoinMainNetParams : AbstractBitcoinNetParams() {
    init {
        interval = INTERVAL
        targetTimespan = TARGET_TIMESPAN

        // 00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL)
//        dumpedPrivateKeyHeader = 204
        addressHeader = 60
        p2shHeader = 122
        port = 8767
//        packetMagic = -0x40f39443
        bip32HeaderP2PKHpub = 0x0488b21e // The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ade4 // The 4 byte header that serializes in base58 to "xprv"
//        genesisBlock.difficultyTarget = 0x1e0ffff0L
//        genesisBlock.setTime(1390095618L)
//        genesisBlock.nonce = 28917698
        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED
        majorityWindow = MAINNET_MAJORITY_WINDOW
        id = ID_MAINNET
//        subsidyDecreaseBlockCount = 210240
//        spendableCoinbaseDepth = 100
//        dnsSeeds = arrayOf(
//            "dnsseed.dash.org"
//        )

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
//        checkpoints[1500] =
//            Sha256Hash.wrap("000000aaf0300f59f49bc3e970bad15c11f961fe2347accffff19d96ec9778e3")


//        // Dash does not have a Http Seeder
//        // If an Http Seeder is set up, add it here.  References: HttpDiscovery
//        httpSeeds = null
//
//        addrSeeds = intArrayOf(

//        )
    }

    override fun getPaymentProtocolId(): String {
        return PAYMENT_PROTOCOL_ID_MAINNET
    }

    companion object {
        const val MAINNET_MAJORITY_WINDOW = 1000
        const val MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950
        const val MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750
    }
}