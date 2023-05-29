package com.tangem.blockchain.common

sealed interface FeeSelectionState {
    object Allows : FeeSelectionState
    object Forbids : FeeSelectionState
// [REDACTED_TODO_COMMENT]
    object Unspecified : FeeSelectionState
}
