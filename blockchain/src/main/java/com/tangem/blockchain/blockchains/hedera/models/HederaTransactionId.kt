package com.tangem.blockchain.blockchains.hedera.models

import com.hedera.hashgraph.sdk.TransactionId
import com.tangem.blockchain.extensions.replaceLast

internal data class HederaTransactionId(
    val transactionId: TransactionId,
    val rawStringId: String,
) {

    internal companion object {
        private const val HEDERA_EXPLORER_LINK_DELIMITER = "-"

        fun fromTransactionId(transactionId: TransactionId): HederaTransactionId {
            val rawStringId = transactionId.toString()
                .replaceFirst("@", HEDERA_EXPLORER_LINK_DELIMITER)
                .replaceLast(".", HEDERA_EXPLORER_LINK_DELIMITER)

            return HederaTransactionId(transactionId = transactionId, rawStringId = rawStringId)
        }

        fun fromRawStringId(id: String): HederaTransactionId {
            val transactionId = id
                .replaceFirst(HEDERA_EXPLORER_LINK_DELIMITER, "@")
                .replaceLast(HEDERA_EXPLORER_LINK_DELIMITER, ".")
                .let(TransactionId::fromString)
            return HederaTransactionId(transactionId = transactionId, rawStringId = id)
        }
    }
}