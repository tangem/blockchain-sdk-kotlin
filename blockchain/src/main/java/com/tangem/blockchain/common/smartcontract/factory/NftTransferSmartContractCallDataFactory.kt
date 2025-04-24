package com.tangem.blockchain.common.smartcontract.factory

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft.NFTSafeTransferERC1155TokenCallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft.NFTSafeTransferERC721TokenCallData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.nft.models.NFTAsset

internal object NftTransferSmartContractCallDataFactory {

    fun create(
        nftAsset: NFTAsset,
        ownerAddress: String,
        destinationAddress: String,
        blockchain: Blockchain,
    ): SmartContractCallData? = when (val identifier = nftAsset.identifier) {
        is NFTAsset.Identifier.EVM -> when (identifier.contractType) {
            NFTAsset.Identifier.EVM.ContractType.ERC1155 -> NFTSafeTransferERC1155TokenCallData(
                nftAsset = nftAsset,
                destinationAddress = destinationAddress,
                ownerAddress = ownerAddress,
            )
            NFTAsset.Identifier.EVM.ContractType.ERC721 -> NFTSafeTransferERC721TokenCallData(
                nftAsset = nftAsset,
                destinationAddress = destinationAddress,
                ownerAddress = ownerAddress,
            )
            else -> error("NFT contract type is not supported for $blockchain")
        }
        else -> null
    }
}