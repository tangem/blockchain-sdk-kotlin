package com.tangem.blockchain.blockchains.near

import com.tangem.blockchain.blockchains.near.network.NearGasPrice
import com.tangem.blockchain.blockchains.near.network.Yocto
import com.tangem.blockchain.blockchains.near.network.api.ProtocolConfigResult

/**
 * Calculation costs of transactions
 *
 * The network buys the execution gas from the sender at `min_gas_purchase_price` and refunds whatever it does not
 * burn, but the whole purchase has to be covered at validation time. On mainnet that price is ten times the block
 * price, so charging the execution part at the block price underestimates the fee by an order of magnitude and a
 * send-all transaction is rejected with `NotEnoughBalance`.
 *
 * @see <a href="https://docs.near.org/concepts/basics/transactions/gas#the-cost-of-common-actions">Docs</a>
[REDACTED_AUTHOR]
 */
internal fun ProtocolConfigResult.calculateSendFundsFee(gasPrice: NearGasPrice, isImplicitAccount: Boolean): Yocto {
    with(runtimeConfig.transactionCosts) {
        val costs = buildList {
            add(actionReceiptCreationConfig)
            add(actionCreationConfig.transferCost)
            if (isImplicitAccount) {
                add(actionCreationConfig.createAccountCost)
                add(actionCreationConfig.addKeyCost.fullAccessCost)
            }
        }

        val sendGas = costs.sumOf { it.sendNotSir }.toBigInteger()
        val executionGas = costs.sumOf { it.execution }.toBigInteger()

        val price = gasPrice.yoctoGasPrice.value
        val receiptPrice = price.max(runtimeConfig.minGasPurchasePrice.toBigInteger())

        return Yocto(sendGas * price + executionGas * receiptPrice)
    }
}