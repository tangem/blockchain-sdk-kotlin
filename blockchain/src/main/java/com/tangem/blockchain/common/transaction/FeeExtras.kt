package com.tangem.blockchain.common.transaction

import java.math.BigInteger

interface FeeExtras

class EthereumFeeExtras(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
) : FeeExtras

object EmptyFeeExtras : FeeExtras