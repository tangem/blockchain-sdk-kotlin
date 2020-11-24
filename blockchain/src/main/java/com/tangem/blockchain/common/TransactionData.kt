package com.tangem.blockchain.common

import java.math.BigDecimal
import java.util.*

data class TransactionData(
        val amount: Amount,
        val fee: Amount?,
        val sourceAddress: String,
        val destinationAddress: String,
        val contractAddress: String? = null,
        var status: TransactionStatus = TransactionStatus.Unconfirmed,
        var date: Calendar? = null,
        var hash: String? = null,
        val extras: TransactionExtras? = null
)


enum class TransactionStatus { Confirmed, Unconfirmed }

enum class TransactionError {
    AmountExceedsBalance,
    FeeExceedsBalance,
    TotalExceedsBalance,
    InvalidAmountValue,
    InvalidFeeValue,
    DustAmount,
    DustChange,
    TezosSendAll
}

enum class TransactionDirection { Incoming, Outgoing }

data class BasicTransactionData(
        val balanceDif: BigDecimal, //change of balance
        val hash: String,
        val date: Calendar?,
        val isConfirmed: Boolean
)

interface TransactionExtras