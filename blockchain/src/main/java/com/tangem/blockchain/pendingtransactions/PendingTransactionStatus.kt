package com.tangem.blockchain.pendingtransactions

/**
 * Status of a pending transaction after checking its state on the blockchain.
 */
sealed interface PendingTransactionStatus {
    /**
     * Transaction is still pending in the mempool.
     */
    data object Pending : PendingTransactionStatus

    /**
     * Transaction has been executed (included in a block).
     */
    data object Executed : PendingTransactionStatus

    /**
     * Transaction was dropped from the mempool (rejected or replaced).
     */
    data object Dropped : PendingTransactionStatus
}