package com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.common.extensions.hexToBytes

/**
 * NFT safe transfer call data in ERC721 - safeTransferFrom(address, address, uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-721#implementations">EIP-721 Safe Transfer</a>
 */
class NFTSafeTransferERC721TokenCallData(
    val nftAsset: NFTAsset,
    val ownerAddress: String,
    val destinationAddress: String,
) : Erc20CallData {

    override val methodId: String = "0x42842e0e"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val identifier = nftAsset.identifier as? NFTAsset.Identifier.EVM ?: error("Wrong blockchain identifier")

            val fromAddressData = ownerAddress.addressWithoutPrefix().hexToFixedSizeBytes()
            val toAddressData = destinationAddress.addressWithoutPrefix().hexToFixedSizeBytes()
            val tokenIdData = identifier.tokenId.toFixedSizeBytes()

            return prefixData + fromAddressData + toAddressData + tokenIdData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(ownerAddress) && ownerAddress.isNotZeroAddress() &&
            blockchain.validateAddress(destinationAddress) && destinationAddress.isNotZeroAddress()
    }
}