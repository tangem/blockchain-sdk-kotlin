package com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.common.extensions.hexToBytes
import org.kethereum.contract.abi.types.leftPadToFixedSize
import java.math.BigInteger

/**
 * NFT safe transfer call data in ERC1155 - safeTransferFrom(address,address,uint256,uint256,bytes
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-1155">EIP1155 Safe Transfer</a>
 */
class NFTSafeTransferERC1155TokenCallData(
    private val nftAsset: NFTAsset,
    val ownerAddress: String,
    val destinationAddress: String,
) : Erc20CallData {
    override val methodId: String = "0xf242432a"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val identifier = nftAsset.identifier as? NFTAsset.Identifier.EVM ?: error("Wrong blockchain identifier")
            val amount = nftAsset.amount ?: error("Invalid nft amount to transfer")

            val fromAddressData = ownerAddress.addressWithoutPrefix().hexToFixedSizeBytes()
            val toAddressData = destinationAddress.addressWithoutPrefix().hexToFixedSizeBytes()
            val amountData = amount.toFixedSizeBytes()
            val tokenIdData = identifier.tokenId.toFixedSizeBytes()
            val dataOffset = DATA_OFFSET.hexToFixedSizeBytes()
            val dataLength = byteArrayOf().leftPadToFixedSize(fixedSize = 32)

            return prefixData + fromAddressData + toAddressData + tokenIdData + amountData + dataOffset + dataLength
        }

    override fun validate(): Boolean {
        val amount = nftAsset.amount ?: return false

        val addressService = EthereumAddressService()
        return addressService.validate(ownerAddress) && ownerAddress.isNotZeroAddress() &&
            addressService.validate(destinationAddress) && destinationAddress.isNotZeroAddress() &&
            amount > BigInteger.ZERO
    }

    companion object {
        private const val DATA_OFFSET = "a0"
    }
}