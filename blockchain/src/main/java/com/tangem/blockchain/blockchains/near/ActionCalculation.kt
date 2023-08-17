package com.tangem.blockchain.blockchains.near

import com.tangem.blockchain.blockchains.near.network.NearGasPrice
import com.tangem.blockchain.blockchains.near.network.Yocto
import com.tangem.blockchain.blockchains.near.network.api.ProtocolConfigResult

/**
 * Calculation costs of transactions
 * @see <a href="https://docs.near.org/concepts/basics/transactions/gas#the-cost-of-common-actions">Docs</a>
[REDACTED_AUTHOR]
 */
internal fun ProtocolConfigResult.calculateSendFundsFee(gasPrice: NearGasPrice): Yocto {
    val transferConfig = runtimeConfig.transactionCosts.actionCreationConfig.transferCost
    val receiptConfig = runtimeConfig.transactionCosts.actionReceiptCreationConfig

    val actionCost = (transferConfig.sendNotSir + receiptConfig.sendNotSir).toBigInteger()
        .times(gasPrice.yoctoGasPrice.value)

    val executionCost = (transferConfig.execution + receiptConfig.execution).toBigInteger()
        .times(gasPrice.yoctoGasPrice.value)

    return Yocto(actionCost + executionCost)
}

internal fun ProtocolConfigResult.calculateCreateAccountFee(gasPrice: NearGasPrice): Yocto {
    val createAccount = runtimeConfig.transactionCosts.actionCreationConfig.createAccountCost
    val receiptConfig = runtimeConfig.transactionCosts.actionReceiptCreationConfig

    val actionCost = (createAccount.sendNotSir + receiptConfig.sendNotSir).toBigInteger()
        .times(gasPrice.yoctoGasPrice.value)

    val executionCost = (createAccount.execution + receiptConfig.execution).toBigInteger()
        .times(gasPrice.yoctoGasPrice.value)

    return Yocto(actionCost + executionCost)
}