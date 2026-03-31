package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result

/**
 * Optional capability interface for UTXO blockchain wallet managers that support
 * dynamic (HD) address management.
 *
 * When enabled, the wallet manager queries balance and UTXOs via XPUB instead of
 * a single address, and sends transactions using per-input derived public keys.
 *
 * Usage from app:
 * ```
 * (walletManager as? DynamicAddressesManager)?.enableDynamicAddresses(xpubString)
 * ```
 */
interface DynamicAddressesManager {

    /** Whether dynamic addresses mode is currently enabled */
    val isDynamicAddressesEnabled: Boolean

    /** Used addresses from the last XPUB query */
    val usedAddresses: List<UsedAddress>

    /**
     * Enable dynamic addresses mode.
     * The wallet manager will switch to XPUB-based balance/UTXO queries
     * and multi-address transaction signing.
     *
     * @param xpub Serialized account-level extended public key string (e.g., "xpub6Bwskq...").
     *   Internally parsed to derive child addresses for HD wallet management.
     */
    fun enableDynamicAddresses(xpub: String)

    /**
     * Disable dynamic addresses mode.
     * Reverts to single-address balance queries and standard signing.
     */
    fun disableDynamicAddresses()

    /**
     * Find the first unused receive address derived from the XPUB.
     * Uses gap-aware search (finds first missing index in the receive chain).
     *
     * @return Derived address info, or null if dynamic addresses not enabled
     */
    fun findFirstUnusedReceiveAddress(): DerivedAddress?

    /**
     * Find the first unused change address derived from the XPUB.
     *
     * @return Derived address info, or null if dynamic addresses not enabled
     */
    fun findFirstUnusedChangeAddress(): DerivedAddress?

    /**
     * Create a consolidation transaction: all UTXOs → single output to base address.
     *
     * @param fee The fee to use
     * @return Transaction data for signing and sending, or error
     */
    fun createConsolidationTransaction(fee: Fee): Result<TransactionData.Uncompiled>

    /**
     * A derived HD address with its public key and position.
     */
    data class DerivedAddress(
        val address: String,
        val publicKey: ByteArray,
        val chain: Int,
        val index: Int,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DerivedAddress
            return address == other.address && chain == other.chain && index == other.index
        }

        override fun hashCode(): Int {
            var result = address.hashCode()
            result = 31 * result + chain
            result = 31 * result + index
            return result
        }
    }
}