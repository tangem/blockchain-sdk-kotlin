package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.Amount

data class TransactionHistoryItem(
    val txHash: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val destinationType: DestinationType,
    val sourceType: SourceType,
    val status: TransactionStatus,
    val type: TransactionType,
    val amount: Amount,
) {

    sealed class DestinationType {
        data class Single(val addressType: AddressType) : DestinationType()
        data class Multiple(val addressTypes: List<AddressType>) : DestinationType()
    }

    sealed class SourceType {

        data class Single(val address: String) : SourceType()
        data class Multiple(val addresses: List<String>) : SourceType()
    }

    sealed class AddressType {
        abstract val address: String

        data class User(override val address: String) : AddressType()
        data class Contract(override val address: String) : AddressType()
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
        data class ContractMethod(val id: String) : TransactionType
    }

    sealed class TransactionStatus {
        object Failed : TransactionStatus()
        object Unconfirmed : TransactionStatus()
        object Confirmed : TransactionStatus()
    }
}
