package com.tangem.blockchain.blockchains.near.network

import com.tangem.blockchain.common.BlockchainSdkError

/**
 * @author Anton Zhilenkov on 31.07.2023.
 */
internal sealed class NearError {

    object UnknownBlock : NearError()
    object InvalidAccount : NearError()
    object UnknownAccount : NearError()
    object UnavailableShard : NearError()
    object NoSyncedBlocks : NearError()
    object UnknownAccessKey : NearError()

    object InvalidTransaction : NearError()
    object UnknownTransaction : NearError()
    object TimeoutError : NearError()

    object ParseError : NearError()
    object InternalError : NearError()
    object UnknownError : NearError()

    companion object {
        fun mapFrom(error: BlockchainSdkError.NearException.Api): NearError = when (error.name) {
            "UNKNOWN_BLOCK" -> UnknownBlock
            "INVALID_ACCOUNT" -> InvalidAccount
            "UNKNOWN_ACCOUNT" -> UnknownAccount
            "UNAVAILABLE_SHARD" -> UnavailableShard
            "NO_SYNCED_BLOCKS" -> NoSyncedBlocks
            "UNKNOWN_ACCESS_KEY" -> UnknownAccessKey

            "INVALID_TRANSACTION" -> InvalidTransaction
            "UNKNOWN_TRANSACTION" -> UnknownTransaction
            "TIMEOUT_ERROR" -> TimeoutError

            "PARSE_ERROR" -> ParseError
            "INTERNAL_ERROR" -> InternalError
            else -> UnknownError
        }
    }
}
