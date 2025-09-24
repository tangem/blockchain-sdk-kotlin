package com.tangem.blockchain.common.smartcontract.factory

import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronTransferTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData

internal object TransferSmartContractCallDataFactory {

    fun create(destinationAddress: String, amount: Amount, blockchain: Blockchain): SmartContractCallData? {
        return when {
            blockchain.isEvm() -> {
                val updatedDestinationAddress = if (blockchain == Blockchain.Decimal) {
                    DecimalAddressService.convertDelAddressToDscAddress(destinationAddress)
                } else {
                    destinationAddress
                }

                when (val amountType = amount.type) {
                    is AmountType.Token -> TransferERC20TokenCallData(
                        destination = updatedDestinationAddress,
                        amount = amount,
                    )
                    is AmountType.TokenYieldSupply -> EthereumYieldSupplySendCallData(
                        tokenContractAddress = amountType.token.contractAddress,
                        destinationAddress = updatedDestinationAddress,
                        amount = amount,
                    )
                    else -> return null
                }
            }
            blockchain == Blockchain.Tron -> {
                if (amount.type !is AmountType.Token) return null
                TronTransferTokenCallData(
                    destination = destinationAddress,
                    amount = amount,
                )
            }
            else -> null
        }
    }
}