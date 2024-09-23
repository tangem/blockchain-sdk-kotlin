package com.tangem.blockchain.transactionhistory.models

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
        data class Validator(override val address: String) : AddressType()
    }

    sealed interface TransactionType {
        data object Transfer : TransactionType
        data class ContractMethod(val id: String) : TransactionType
        data class ContractMethodName(val name: String) : TransactionType

        sealed interface TronStakingTransactionType : TransactionType {
            data object FreezeBalanceV2Contract : TronStakingTransactionType
            data object UnfreezeBalanceV2Contract : TronStakingTransactionType
            data class VoteWitnessContract(val validatorAddress: String) : TronStakingTransactionType
            data object WithdrawBalanceContract : TronStakingTransactionType
        }
    }

    sealed class TransactionStatus {
        data object Failed : TransactionStatus()
        data object Unconfirmed : TransactionStatus()
        data object Confirmed : TransactionStatus()
    }
}