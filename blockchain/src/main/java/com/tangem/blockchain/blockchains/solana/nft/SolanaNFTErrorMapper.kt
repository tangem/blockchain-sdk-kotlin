package com.tangem.blockchain.blockchains.solana.nft

import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.logging.Logger

/**
 * Metaplex Core rejects with bare custom program errors, so the code in the RPC message is the only thing that
 * tells a stale NFT list from a refusing plugin.
 */
internal object SolanaNFTErrorMapper {

    private val CUSTOM_PROGRAM_ERROR = Regex("custom program error: 0x([0-9a-fA-F]+)")
    private const val INVALID_AUTHORITY = 0x9
    private const val INVALID_COLLECTION = 0x13
    private const val MISSING_COLLECTION = 0x19
    private const val NO_APPROVALS = 0x1a
    private const val TAG = "SolanaNFTErrorMapper"

    fun mapCoreError(error: BlockchainError): BlockchainError {
        if (error !is BlockchainSdkError.Solana.Api) return error

        val code = CUSTOM_PROGRAM_ERROR.find(error.customMessage)?.groupValues?.get(1)?.toIntOrNull(radix = 16)
        return when (code) {
            NO_APPROVALS -> BlockchainSdkError.Solana.NFTNotOwner
            INVALID_AUTHORITY -> BlockchainSdkError.Solana.NFTTransferRejected
            INVALID_COLLECTION, MISSING_COLLECTION -> {
                Logger.e(TAG, "Core transfer was built with a wrong collection, code=$code")
                BlockchainSdkError.Solana.NFTInvalidCollection
            }
            else -> error
        }
    }
}