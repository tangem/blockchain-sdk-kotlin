package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.blockchains.ethereum.EthereumFeesCalculator
import com.tangem.blockchain.common.Amount
import wallet.core.jni.proto.Cosmos.Fee
import java.math.BigDecimal
import java.math.BigInteger

sealed class TransactionFee {

    data class Choosable(
        val minimum: Amount,
        val normal: Amount,
        val priority: Amount,
        val additionalData: EthereumAdditionalDataProvider = EthereumAdditionalDataStub,
    ) : TransactionFee(), EthereumAdditionalDataProvider by additionalData

    data class Single(
        val normal: Amount,
    ) : TransactionFee()
}

//class Fee(val amount: Amount, val)

interface FeeExtras

class EthereumExtras(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
) : FeeExtras

class EmptyExtras : FeeExtras

interface EthereumAdditionalDataProvider {

    val gasLimit: BigInteger
    val gasPrice: BigInteger
    val minimalMultiplier: BigDecimal
    val normalMultiplier: BigDecimal
    val priorityMultiplier: BigDecimal
}

class EthereumAdditionalDataProviderImpl(
    override val gasPrice: BigInteger,
    override val gasLimit: BigInteger,
    override val minimalMultiplier: BigDecimal = EthereumFeesCalculator.minimalMultiplier,
    override val normalMultiplier: BigDecimal = EthereumFeesCalculator.normalMultiplier,
    override val priorityMultiplier: BigDecimal = EthereumFeesCalculator.priorityMultiplier,
) : EthereumAdditionalDataProvider

object EthereumAdditionalDataStub : EthereumAdditionalDataProvider {

    override val gasLimit: BigInteger = BigInteger.ZERO
    override val gasPrice: BigInteger = BigInteger.ZERO
    override val minimalMultiplier: BigDecimal = BigDecimal.ZERO
    override val normalMultiplier: BigDecimal = BigDecimal.ZERO
    override val priorityMultiplier: BigDecimal = BigDecimal.ZERO
}