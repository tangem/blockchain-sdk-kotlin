package com.tangem.blockchain.common

interface IncompleteBalanceProvider {

    val unspentsLimit: Int

    val isBalanceIncomplete: Boolean
}