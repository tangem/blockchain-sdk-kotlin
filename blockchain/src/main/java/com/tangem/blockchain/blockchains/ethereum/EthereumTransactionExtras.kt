package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import java.math.BigInteger

data class EthereumTransactionExtras(
    val smartContract: SmartContractMethod? = null,
    val gasLimit: BigInteger? = null,
    val nonce: BigInteger? = null,
) : TransactionExtras