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
        var hash: String? = null
)


enum class TransactionStatus { Confirmed, Unconfirmed }

enum class TransactionError { WrongAmount, WrongFee, WrongTotal, DustAmount, DustChange }

data class BasicTransactionData(
        val balanceDif: BigDecimal, //change of balance
        val hash: String,
        val date: Calendar?,
        val isConfirmed: Boolean
)