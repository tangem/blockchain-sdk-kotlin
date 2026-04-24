package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.address.AddressType
import java.math.BigDecimal

data class XpubInfoResponse(
    val balance: BigDecimal,
    val unspentOutputs: List<BitcoinUnspentOutput>,
    val usedAddresses: List<UsedAddress>,
    val hasUnconfirmed: Boolean,
    val recentTransactions: List<BasicTransactionData>,
)

data class UsedAddress(
    val address: String,
    val derivationPath: String,
    val balance: BigDecimal,
    val scriptType: AddressType = AddressType.Default,
)