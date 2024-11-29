package com.tangem.blockchain.blockchains.kaspa.krc20

import com.tangem.blockchain.blockchains.kaspa.krc20.model.IncompleteTokenTransactionParams
import com.tangem.blockchain.common.TransactionExtras

data class KaspaKRC20TransactionExtras(
    val incompleteTokenTransactionParams: IncompleteTokenTransactionParams,
) : TransactionExtras