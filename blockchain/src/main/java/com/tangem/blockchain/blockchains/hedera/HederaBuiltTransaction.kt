package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.Transaction

internal data class HederaBuiltTransaction<T : Transaction<T>>(
    val transaction: T,
    val signatures: List<ByteArray>,
)