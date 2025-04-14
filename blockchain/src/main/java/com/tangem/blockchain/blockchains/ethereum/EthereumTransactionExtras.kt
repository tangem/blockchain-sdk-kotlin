package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.TransactionExtras
import java.math.BigInteger

data class EthereumTransactionExtras(
    val data: ByteArray? = null,
    val gasLimit: BigInteger? = null,
    val nonce: BigInteger? = null,
) : TransactionExtras