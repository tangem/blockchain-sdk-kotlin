package com.tangem.blockchain.common.smartcontract

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.factory.ApprovalSmartContractFactory
import com.tangem.blockchain.common.smartcontract.factory.TransferSmartContractFactory

/**
 * Factory provides different smart contracts depending on blockchain
 */
object SmartContractProviderFactory {

    fun getApprovalSmartContract(spenderAddress: String, amount: Amount?, blockchain: Blockchain) =
        ApprovalSmartContractFactory.create(
            spenderAddress = spenderAddress,
            amount = amount,
            blockchain = blockchain,
        )

    fun getTokenTransferSmartContract(destinationAddress: String, amount: Amount, blockchain: Blockchain) =
        TransferSmartContractFactory.create(
            destinationAddress = destinationAddress,
            amount = amount,
            blockchain = blockchain,
        )
}