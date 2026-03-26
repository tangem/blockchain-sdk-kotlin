package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

/**
 * Manages HD address derivation for UTXO blockchains with Dynamic Addresses (multi-address) support.
 *
 * Given an account-level XPUB (e.g., at m/84'/0'/0' for BTC), derives child addresses
 * using BIP32 non-hardened derivation:
 * - Receive addresses: chain=0, e.g., m/84'/0'/0'/0/0, m/84'/0'/0'/0/1, ...
 * - Change addresses:  chain=1, e.g., m/84'/0'/0'/1/0, m/84'/0'/0'/1/1, ...
 *
 * Supports gap-aware "first unused" address finding per BIP44 standard.
 */
class BitcoinDynamicAddressesManager(
    private val extendedPublicKey: ExtendedPublicKey,
    private val blockchain: Blockchain,
) {

    /**
     * Derive a public key at chain/index using two-level BIP32 CKDpub.
     * XPUB (account-level) -> chain key -> index key
     */
    fun derivePublicKey(chain: Int, index: Int): ByteArray {
        val chainNode = DerivationNode.NonHardened(chain.toLong())
        val indexNode = DerivationNode.NonHardened(index.toLong())

        val chainKey = extendedPublicKey.derivePublicKey(chainNode)
        val indexKey = chainKey.derivePublicKey(indexNode)

        return indexKey.publicKey // already compressed (33 bytes)
    }

    /**
     * Derive address at chain/index.
     * Address format depends on blockchain: bech32 for BTC/LTC, P2PKH for DOGE/DASH/RVN, cashaddr for BCH.
     */
    fun deriveAddress(chain: Int, index: Int): DynamicAddressesManager.DerivedAddress {
        val publicKey = derivePublicKey(chain, index)
        val address = makeAddress(publicKey)

        return DynamicAddressesManager.DerivedAddress(
            address = address,
            publicKey = publicKey,
            chain = chain,
            index = index,
        )
    }

    /**
     * Find first unused receive address (chain=0) using gap-aware search.
     * Parses used addresses to find the first missing index in the receive chain.
     *
     * Example: used indices [0,1,2,3,7] → returns address at index 4.
     */
    fun findFirstUnusedReceiveAddress(usedAddresses: List<UsedAddress>): DynamicAddressesManager.DerivedAddress {
        return findFirstUnusedAddress(chain = RECEIVE_CHAIN, usedAddresses = usedAddresses)
    }

    /**
     * Find first unused change address (chain=1) using gap-aware search.
     */
    fun findFirstUnusedChangeAddress(usedAddresses: List<UsedAddress>): DynamicAddressesManager.DerivedAddress {
        return findFirstUnusedAddress(chain = CHANGE_CHAIN, usedAddresses = usedAddresses)
    }

    private fun findFirstUnusedAddress(
        chain: Int,
        usedAddresses: List<UsedAddress>,
    ): DynamicAddressesManager.DerivedAddress {
        val usedIndices = usedAddresses
            .mapNotNull { usedAddress ->
                runCatching { parseChainAndIndex(usedAddress.path) }.getOrNull()
            }
            .filter { it.first == chain }
            .map { it.second }
            .toSet()

        if (usedIndices.isEmpty()) {
            return deriveAddress(chain, 0)
        }

        // Find first gap in sequence 0, 1, 2, ...
        val maxUsedIndex = usedIndices.max()
        for (i in 0..maxUsedIndex) {
            if (i !in usedIndices) {
                return deriveAddress(chain, i)
            }
        }

        // No gap found — return next after max
        return deriveAddress(chain, maxUsedIndex + 1)
    }

    private fun makeAddress(publicKey: ByteArray): String {
        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.Litecoin -> {
                // SegWit (bech32) address
                val addresses = BitcoinAddressService(blockchain).makeAddresses(publicKey)
                addresses.find { it.type == AddressType.Default }?.value
                    ?: addresses.first().value
            }
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                BitcoinCashAddressService(blockchain).makeAddress(publicKey)
            }
            else -> {
                // Legacy P2PKH address (Dogecoin, Dash, Ravencoin, etc.)
                BitcoinAddressService(blockchain).makeAddress(publicKey)
            }
        }
    }

    companion object {
        const val RECEIVE_CHAIN = 0
        const val CHANGE_CHAIN = 1
        private const val MIN_PATH_SEGMENTS = 3

        /**
         * Parse BIP44/84 derivation path to extract chain and index.
         * Path format: "m/purpose'/coin'/account'/chain/index"
         * Example: "m/84'/0'/0'/0/5" → Pair(chain=0, index=5)
         */
        fun parseChainAndIndex(path: String): Pair<Int, Int> {
            val nodes = DerivationPath(path).nodes
            require(nodes.size >= MIN_PATH_SEGMENTS) { "Path must have at least chain and index segments: $path" }

            val chain = nodes[nodes.size - 2].index.toInt()
            val index = nodes.last().index.toInt()

            return Pair(chain, index)
        }
    }
}