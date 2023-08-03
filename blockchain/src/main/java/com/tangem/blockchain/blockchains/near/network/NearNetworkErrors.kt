package com.tangem.blockchain.blockchains.near.network

/**
[REDACTED_AUTHOR]
 */
internal sealed class NearError {

    object UnknownBlock : NearError()
    object InvalidAccount : NearError()
    object UnknownAccount : NearError()
    object UnavailableShard : NearError()
    object NoSyncedBlocks : NearError()

    object InvalidTransaction : NearError()
    object UnknownTransaction : NearError()
    object TimeoutError : NearError()

    object ParseError : NearError()
    object Internal : NearError()
}