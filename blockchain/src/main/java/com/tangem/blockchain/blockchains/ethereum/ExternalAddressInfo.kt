package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData

/**
 * The state of an external address after an update: what [ExternalAddressBalanceProvider] knows about it.
 *
 * @property address            the observed address
 * @property amounts            coin and token balances, one entry per [AmountType]
 * @property recentTransactions transactions attributed to the address by the host, unconfirmed ones included
 */
class ExternalAddressInfo(
    val address: String,
    val amounts: Map<AmountType, Amount>,
    val recentTransactions: List<TransactionData.Uncompiled>,
)