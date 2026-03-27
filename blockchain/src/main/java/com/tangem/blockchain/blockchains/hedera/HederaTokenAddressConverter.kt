package com.tangem.blockchain.blockchains.hedera

class HederaTokenAddressConverter {

    /**
     * Converts a token address to Hedera Token ID format (0.0.XXXXXXX).
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
}