package com.tangem.blockchain.blockchains.hedera

class HederaTokenAddressConverter {

    /**
     * Converts a token address to Hedera Token ID format (0.0.XXXXXXX) synchronously.
     *
     * If the input is an EVM address (0x...):
     * - First 10 bytes zeros: convert hex to decimal, prepend "0.0."
     * - Otherwise: return as-is (needs network resolution via GET /contracts/{evm_address})
     *
     * If the input is already in 0.0.X format, returns as-is.
     */
    fun convertToTokenId(address: String): String {
        // If it looks like an EVM address, try to convert directly
        if (address.startsWith("0x") || address.startsWith("0X")) {
            val accountId = HederaUtils.evmAddressToAccountId(address)
            if (accountId != null) {
                return accountId
            }
            // Non-zero first bytes — can't convert locally, return as-is for network resolution
            return address
        }

        return HederaUtils.createTokenId(address).toString()
    }

    /**
     * Converts a token address to Hedera Token ID format (0.0.XXXXXXX),
     * resolving non-zero-prefix EVM addresses via network call.
     *
     * @param contractInfoResolver resolves an EVM address to a contract ID (0.0.X) via
     *   Hedera Mirror Node (`GET /api/v1/contracts/{evm_address}`).
     *   Returns null if resolution fails.
     */
    suspend fun resolveTokenId(
        address: String,
        contractInfoResolver: suspend (evmAddress: String) -> String?,
    ): String {
        val localResult = convertToTokenId(address)
        if (!localResult.startsWith("0x", ignoreCase = true)) {
            return localResult
        }
        return contractInfoResolver(address) ?: localResult
    }
}