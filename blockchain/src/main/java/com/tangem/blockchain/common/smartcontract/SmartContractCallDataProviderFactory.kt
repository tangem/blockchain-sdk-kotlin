package com.tangem.blockchain.common.smartcontract

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.factory.ApprovalSmartContractCallDataFactory
import com.tangem.blockchain.common.smartcontract.factory.NftTransferSmartContractCallDataFactory
import com.tangem.blockchain.common.smartcontract.factory.TransferSmartContractCallDataFactory
import com.tangem.blockchain.nft.models.NFTAsset

/**
 * Factory provides different smart contract call data depending on blockchain
 */
object SmartContractCallDataProviderFactory {

    fun getApprovalCallData(spenderAddress: String, amount: Amount?, blockchain: Blockchain) =
        ApprovalSmartContractCallDataFactory.create(
            spenderAddress = spenderAddress,
            amount = amount,
            blockchain = blockchain,
        )

    fun getTokenTransferCallData(destinationAddress: String, amount: Amount, blockchain: Blockchain) =
        TransferSmartContractCallDataFactory.create(
            destinationAddress = destinationAddress,
            amount = amount,
            blockchain = blockchain,
        )

    fun getNFTTransferCallData(
        nftAsset: NFTAsset,
        ownerAddress: String,
        destinationAddress: String,
        blockchain: Blockchain,
    ) = NftTransferSmartContractCallDataFactory.create(
        nftAsset = nftAsset,
        ownerAddress = ownerAddress,
        destinationAddress = destinationAddress,
        blockchain = blockchain,
    )
}