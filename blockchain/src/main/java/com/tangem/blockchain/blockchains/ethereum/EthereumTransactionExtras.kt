package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.TransactionExtras
import java.math.BigInteger

class EthereumTransactionExtras(
    val data: ByteArray,
    val gasLimit: BigInteger? = null,
    val nonce: BigInteger? = null,
) : TransactionExtras
