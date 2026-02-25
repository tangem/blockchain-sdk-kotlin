package com.tangem.blockchain.common.address

import com.tangem.blockchain.extensions.Result

/**
 * Provider for wallet address information.
 * Used by protocols like WalletConnect to query available addresses.
 */
interface AddressProvider {
    /**
     * Gets available addresses for the wallet with filtering options.
     *
     * @param filterOptions Optional blockchain-specific filter (e.g., intention types)
     * @return Success with list of addresses with metadata, or Failure with error
     */
    fun getAddresses(filterOptions: Any? = null): Result<List<AddressInfo>>
}

/**
 * Address information with metadata.
 *
 * @property address Blockchain address
 * @property publicKey Optional public key as hex string
 * @property derivationPath Optional derivation path
 * @property metadata Optional blockchain-specific metadata
 */
data class AddressInfo(
    val address: String,
    val publicKey: String? = null,
    val derivationPath: String? = null,
    val metadata: Map<String, Any>? = null,
)