package com.tangem.blockchain.blockchains.bitcoin.address

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.AddressIntention
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressInfo
import com.tangem.blockchain.common.address.AddressProvider
import com.tangem.blockchain.extensions.Result

/**
 * Bitcoin implementation of address provider.
 *
 * Provides wallet addresses with filtering support for Bitcoin-specific
 * intentions (payment/ordinal).
 *
 * @property wallet The Bitcoin wallet instance
 */
internal class BitcoinAddressProvider(
    private val wallet: Wallet,
) : AddressProvider {

    override fun getAddresses(filterOptions: Any?): Result<List<AddressInfo>> {
        val intentions = parseIntentions(filterOptions)

        // If only "ordinal" intention is requested, return empty list
        if (isOnlyOrdinalIntention(intentions)) {
            return Result.Success(emptyList())
        }

        // Return all wallet addresses with "payment" intention
        val addresses = wallet.addresses.map { address ->
            AddressInfo(
                address = address.value,
                publicKey = null, // Don't expose public key for security
                derivationPath = null,
                metadata = mapOf("intention" to AddressIntention.PAYMENT.toApiString()),
            )
        }

        return Result.Success(addresses)
    }

    /**
     * Parses intentions from filter options.
     */
    private fun parseIntentions(filterOptions: Any?): List<AddressIntention> {
        return when (filterOptions) {
            is List<*> -> {
                filterOptions.mapNotNull {
                    when (it) {
                        is String -> AddressIntention.fromString(it)
                        else -> null
                    }
                }
            }
            else -> listOf(AddressIntention.PAYMENT)
        }
    }

    /**
     * Checks if only ordinal intention is requested.
     */
    private fun isOnlyOrdinalIntention(intentions: List<AddressIntention>): Boolean {
        return intentions.size == 1 && intentions.contains(AddressIntention.ORDINAL)
    }
}