package com.tangem.blockchain.common.smartcontract.factory

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronApprovalTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData

internal object ApprovalSmartContractCallDataFactory {

    fun create(spenderAddress: String, amount: Amount?, blockchain: Blockchain): SmartContractCallData = when {
        blockchain.isEvm() -> ApprovalERC20TokenCallData(
            spenderAddress = spenderAddress,
            amount = amount,
        )
        blockchain == Blockchain.Tron -> TronApprovalTokenCallData(
            spenderAddress = spenderAddress,
            amount = amount,
        )
        else -> error("Approval call data is not supported for $blockchain")
    }
}