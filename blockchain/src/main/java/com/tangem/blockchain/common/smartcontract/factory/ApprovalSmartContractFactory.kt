package com.tangem.blockchain.common.smartcontract.factory

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenMethod
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronApprovalTokenMethod
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractMethod

internal object ApprovalSmartContractFactory {

    fun create(spenderAddress: String, amount: Amount?, blockchain: Blockchain): SmartContractMethod = when {
        blockchain.isEvm() -> ApprovalERC20TokenMethod(
            spenderAddress = spenderAddress,
            amount = amount,
        )
        blockchain == Blockchain.Tron -> TronApprovalTokenMethod(
            spenderAddress = spenderAddress,
            amount = amount,
        )
        else -> error("Approval smart contract is not supported for $blockchain")
    }
}