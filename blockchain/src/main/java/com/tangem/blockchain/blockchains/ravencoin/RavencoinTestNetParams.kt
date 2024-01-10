package com.tangem.blockchain.blockchains.ravencoin

import org.bitcoinj.core.Utils
import org.bitcoinj.params.AbstractBitcoinNetParams

@Suppress("MagicNumber")
class RavencoinTestNetParams : AbstractBitcoinNetParams() {
    init {
        interval = INTERVAL
        targetTimespan = TARGET_TIMESPAN

        // 00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL)
        addressHeader = 111
        p2shHeader = 196
        port = 18767
        bip32HeaderP2PKHpub = 0x0488b21e // The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ade4 // The 4 byte header that serializes in base58 to "xprv"
        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED
        majorityWindow = TESTNET_MAJORITY_WINDOW
        id = ID_MAINNET
        dnsSeeds = arrayOf(
            "seed-testnet-raven.bitactivate.com",
            "seed-testnet-raven.ravencoin.com",
            "seed-testnet-raven.ravencoin.org",
        )
    }

    override fun getPaymentProtocolId(): String {
        return PAYMENT_PROTOCOL_ID_TESTNET
    }

    private companion object {
        private const val TESTNET_MAJORITY_WINDOW = 100
        private const val TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75
        private const val TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51
    }
}
