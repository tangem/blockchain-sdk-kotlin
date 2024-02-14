package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.TransferTransaction

data class HederaBuiltTransaction(
    val transferTransaction: TransferTransaction,
    val signatures: List<ByteArray>,
)