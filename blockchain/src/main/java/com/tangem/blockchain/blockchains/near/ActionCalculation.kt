package com.tangem.blockchain.blockchains.near

import com.tangem.blockchain.blockchains.near.network.NearGasPrice
import com.tangem.blockchain.blockchains.near.network.Yocto
import com.tangem.blockchain.blockchains.near.network.api.ProtocolConfigResult

/**
 * Calculation costs of transactions
 * @see <a href="https://docs.near.org/concepts/basics/transactions/gas#the-cost-of-common-actions">Docs</a>
 * @author Anton Zhilenkov on 17.08.2023.
 */
internal fun ProtocolConfigResult.calculateSendFundsFee(gasPrice: NearGasPrice, isImplicitAccount: Boolean): Yocto {
    with(runtimeConfig.transactionCosts) {
        val receiptCreationCost = actionReceiptCreationConfig.cost

        val transferCost = actionCreationConfig.transferCost.cost

        val additionalCosts = if (isImplicitAccount) {
            actionCreationConfig.createAccountCost.cost + actionCreationConfig.addKeyCost.fullAccessCost.cost
        } else {
            0
        }

        val gas = (receiptCreationCost + transferCost + additionalCosts).toBigInteger()
        val gasPriceValue = gasPrice.yoctoGasPrice.value

        return Yocto(gas * gasPriceValue)
    }
}

internal fun ProtocolConfigResult.calculateCreateAccountFee(gasPrice: NearGasPrice): Yocto {
    val createAccount = runtimeConfig.transactionCosts.actionCreationConfig.createAccountCost.cost

    return Yocto(createAccount.toBigInteger() * gasPrice.yoctoGasPrice.value)
}

private val ProtocolConfigResult.CostConfig.cost: Long
    get() = execution + sendNotSir
