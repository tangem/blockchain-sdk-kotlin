package com.tangem.blockchain.common.memo

sealed interface MemoState {
    data object Valid : MemoState
    data object Invalid : MemoState
    data object NotSupported : MemoState
}