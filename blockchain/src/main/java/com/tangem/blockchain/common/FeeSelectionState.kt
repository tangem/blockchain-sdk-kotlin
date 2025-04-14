package com.tangem.blockchain.common

sealed interface FeeSelectionState {
    object Allows : FeeSelectionState
    object Forbids : FeeSelectionState
    // TODO [REDACTED_TASK_KEY] delete this one
    object Unspecified : FeeSelectionState
}