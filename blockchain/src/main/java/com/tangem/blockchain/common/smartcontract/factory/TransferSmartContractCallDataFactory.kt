package com.tangem.blockchain.common.smartcontract.factory

import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronTransferTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData

internal object TransferSmartContractCallDataFactory {

    fun create(destinationAddress: String, amount: Amount, blockchain: Blockchain): SmartContractCallData? {
        if (amount.type !is AmountType.Token) return null

        return when {
            blockchain.isEvm() -> {
                val updatedDestinationAddress = if (blockchain == Blockchain.Decimal) {
                    DecimalAddressService.convertDelAddressToDscAddress(destinationAddress)
                } else {
                    destinationAddress
                }
                TransferERC20TokenCallData(
                    destination = updatedDestinationAddress,
                    amount = amount,
                )
            }
            blockchain == Blockchain.Tron -> TronTransferTokenCallData(
                destination = destinationAddress,
                amount = amount,
            )
            else -> null
        }
    }
}