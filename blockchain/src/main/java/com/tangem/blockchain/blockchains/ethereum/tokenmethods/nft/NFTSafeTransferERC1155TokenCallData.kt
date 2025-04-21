package com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.removeLeadingZeros
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.common.extensions.hexToBytes
import org.kethereum.contract.abi.types.leftPadToFixedSize

/**
 * NFT safe transfer call data in ERC1155 - safeTransferFrom(address,address,uint256,uint256,bytes
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-1155">EIP1155 Safe Transfer</a>
 */
class NFTSafeTransferERC1155TokenCallData(
    private val nftAsset: NFTAsset,
    val ownerAddress: String,
    val destinationAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0xf242432a"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val identifier = nftAsset.identifier as? NFTAsset.Identifier.EVM ?: error("Wrong blockchain identifier")
            val amount = nftAsset.amount ?: error("Invalid nft amount to transfer")

            val fromAddressData = ownerAddress.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val toAddressData = destinationAddress.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val amountData = amount.toByteArray().leftPadToFixedSize(fixedSize = 32)
            val tokenIdData = identifier.tokenId.toByteArray().removeLeadingZeros().leftPadToFixedSize(fixedSize = 32)
            val dataOffset = DATA_OFFSET.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val dataLength = byteArrayOf().leftPadToFixedSize(fixedSize = 32)

            return prefixData + fromAddressData + toAddressData + tokenIdData + amountData + dataOffset + dataLength
        }

    companion object {
        private const val DATA_OFFSET = "a0"
    }
}