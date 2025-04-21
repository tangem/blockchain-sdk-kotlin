package com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.removeLeadingZeros
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.common.extensions.hexToBytes
import org.kethereum.contract.abi.types.leftPadToFixedSize

/**
 * NFT safe transfer call data in ERC721 - safeTransferFrom(address, address, uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-721#implementations">EIP-721 Safe Transfer</a>
 */
class NFTSafeTransferERC721TokenCallData(
    val nftAsset: NFTAsset,
    val ownerAddress: String,
    val destinationAddress: String,
) : SmartContractCallData {

    override val methodId: String = "0x42842e0e"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val identifier = nftAsset.identifier as? NFTAsset.Identifier.EVM ?: error("Wrong blockchain identifier")

            val fromAddressData = ownerAddress.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val toAddressData = destinationAddress.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val tokenIdData = identifier.tokenId.toByteArray().removeLeadingZeros().leftPadToFixedSize(fixedSize = 32)

            return prefixData + fromAddressData + toAddressData + tokenIdData
        }
}