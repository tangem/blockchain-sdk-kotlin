package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallData

class TronTransactionExtras(
    val callData: SmartContractCallData,
) : TransactionExtras