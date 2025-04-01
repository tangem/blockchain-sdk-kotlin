package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import java.math.BigInteger

data class EthereumTransactionExtras(
    val callData: SmartContractCallData? = null,
    val gasLimit: BigInteger? = null,
    val nonce: BigInteger? = null,
) : TransactionExtras