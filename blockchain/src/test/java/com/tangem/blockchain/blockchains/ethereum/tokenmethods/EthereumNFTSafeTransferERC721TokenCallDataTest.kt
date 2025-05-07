package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.nft.NFTSafeTransferERC721TokenCallData
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal
import java.math.BigInteger

internal class EthereumNFTSafeTransferERC721TokenCallDataTest {

    private val signature = "0x42842e0e".hexToBytes()
    private val ownerAddress = "0x2Cf9DA532E8c27d464096a39C0F14E3804EA91d4"
    private val ownerAddressData = "0000000000000000000000002cf9da532e8c27d464096a39c0f14e3804ea91d4".hexToBytes()
    private val destinationAddress = "0x06012c8cf97BEaD5deAe237070F9587f8E7A266d"
    private val destinationAddressData = "00000000000000000000000006012c8cf97bead5deae237070f9587f8e7a266d".hexToBytes()

    private val nftAsset = NFTAsset(
        identifier = NFTAsset.Identifier.EVM(
            tokenId = BigInteger.ONE,
            tokenAddress = "0xA1C0293e4811C856bd9D8Bf2Add9f7fadb482b30",
            contractType = NFTAsset.Identifier.EVM.ContractType.ERC721,
        ),
        collectionIdentifier = NFTCollection.Identifier.EVM(
            tokenAddress = "0xA1C0293e4811C856bd9D8Bf2Add9f7fadb482b30",
        ),
        blockchainId = "polygon",
        contractType = "ERC1155",
        owner = "0x2Cf9DA532E8c27d464096a39C0F14E3804EA91d4",
        name = "Test",
        description = "",
        amount = BigInteger.ONE,
        decimals = 0,
        salePrice = NFTAsset.SalePrice(
            value = BigDecimal.ZERO,
            symbol = "ETH",
            decimals = 18,
        ),
        rarity = NFTAsset.Rarity(
            rank = "",
            label = "",
        ),
        media = NFTAsset.Media(
            mimetype = "",
            url = "",
        ),
        traits = listOf(
            NFTAsset.Trait(
                name = "",
                value = "",
            ),
        ),
    )

    @Test
    fun makeNFTSafeTransferData() {
        val tokenIdData = "0000000000000000000000000000000000000000000000000000000000000001".hexToBytes()
        val expected = signature + ownerAddressData + destinationAddressData + tokenIdData

        val actual = NFTSafeTransferERC721TokenCallData(
            nftAsset = nftAsset,
            ownerAddress = ownerAddress,
            destinationAddress = destinationAddress,
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }
}