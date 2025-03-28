package com.tangem.blockchain.common.smartcontract.factory

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenMethod
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronTransferTokenMethod
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractMethod

internal object TransferSmartContractFactory {

    fun create(destinationAddress: String, amount: Amount, blockchain: Blockchain): SmartContractMethod? {
        if (amount.type !is AmountType.Token) return null

        return when {
            blockchain.isEvm() -> TransferERC20TokenMethod(
                destination = destinationAddress,
                amount = amount,
            )
            blockchain == Blockchain.Tron -> TronTransferTokenMethod(
                destination = destinationAddress,
                amount = amount,
            )
            else -> null
        }
    }
}