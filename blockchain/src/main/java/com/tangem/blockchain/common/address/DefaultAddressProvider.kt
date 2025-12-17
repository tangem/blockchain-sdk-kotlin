package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result

/**
 * Default address provider that returns wallet addresses without filtering.
 */
internal class DefaultAddressProvider(
    private val wallet: Wallet,
) : AddressProvider {
    override fun getAddresses(filterOptions: Any?): Result<List<AddressInfo>> {
        val addresses = wallet.addresses.map { address ->
            AddressInfo(
                address = address.value,
                publicKey = null, // Don't expose by default for security
                derivationPath = null,
                metadata = null,
            )
        }
        return Result.Success(addresses)
    }
}