package com.tangem.blockchain.common

import com.tangem.blockchain.common.transaction.Fee
import java.math.BigDecimal
import java.util.Calendar
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class TransactionData {

    open var status: TransactionStatus = TransactionStatus.Unconfirmed
    open var hash: String? = null

    data class Uncompiled(
        val amount: Amount,
        val fee: Fee?,
        val sourceAddress: String,
        val destinationAddress: String,
        override var status: TransactionStatus = TransactionStatus.Unconfirmed,
        override var hash: String? = null,
        var date: Calendar? = null,
        val extras: TransactionExtras? = null,
        val contractAddress: String? = (amount.type as? AmountType.Token)?.token?.contractAddress,
    ) : TransactionData()

    data class Compiled(
        val value: Data,
        val fee: Fee? = null,
        val amount: Amount? = null,
        override var status: TransactionStatus = TransactionStatus.Unconfirmed,
        override var hash: String? = null,
    ) : TransactionData() {

        sealed class Data {
            data class RawString(
                val data: String,
            ) : Data()

            data class Bytes(
                val data: ByteArray,
            ) : Data()
        }
    }

    fun updateHash(hash: String): TransactionData {
        return when (this) {
            is Uncompiled -> {
                copy(hash = hash)
            }
            is Compiled -> {
                copy(hash = hash)
            }
        }
    }

    fun updateStatus(status: TransactionStatus): TransactionData {
        return when (this) {
            is Uncompiled -> {
                copy(status = status)
            }
            is Compiled -> {
                copy(status = status)
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    fun requireUncompiled(): Uncompiled {
        contract {
            returns() implies (this@TransactionData is Uncompiled)
        }
        return this as? Uncompiled
            ?: error("This blockchain doesn't support compiled transactions processing")
    }
}

enum class TransactionStatus { Confirmed, Unconfirmed }

enum class TransactionError { // TODO: add address validation?
    AmountExceedsBalance,
    AmountLowerExistentialDeposit,
    FeeExceedsBalance,
    TotalExceedsBalance,
    InvalidAmountValue,
    InvalidFeeValue,
    DustAmount,
    DustChange,
    TezosSendAll,
}

enum class TransactionDirection { Incoming, Outgoing }

data class BasicTransactionData(
    val balanceDif: BigDecimal, // change of balance
    val hash: String,
    val date: Calendar?,
    val isConfirmed: Boolean,
    val destination: String = "unknown",
    val source: String = "unknown",
)

interface TransactionExtras