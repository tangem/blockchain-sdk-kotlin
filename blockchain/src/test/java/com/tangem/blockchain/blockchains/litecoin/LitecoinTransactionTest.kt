package com.tangem.blockchain.blockchains.litecoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionTest
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.libdohj.params.LitecoinMainNetParams

class LitecoinTransactionTest {

    private val blockchain = Blockchain.Litecoin
    private val networkParameters = LitecoinMainNetParams()
    private val dustValue = 0.00001.toBigDecimal()

    @Test
    fun buildCorrectTransaction() {
        // arrange
        val walletPublicKey = (
            "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A5456" +
                "18C174251B984DF841F49D2376F"
            ).hexToBytes()
        val signature = (
            "88E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B2E918EF79E15E2747D4E00C5" +
                "5D69FA0B8ADFAFD07F41144F81337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D" +
                "44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
            ).hexToBytes()
        val sendValue = "10000".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "ltc1qd3s8qjxp00k9rkqmfd8z8qq5xnwt4cxn5y5lem"

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val address = addresses.find { it.type == AddressType.Legacy }!!.value
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs =
            BitcoinTransactionTest.prepareTwoUnspentOutputs(listOf(address), networkParameters)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = address,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
        )

        val expectedHashToSign1 = "1C0490CECD7A96F9B2DCADF01122E49517E947A0133BF14D58F45D4A75003D67"
            .hexToBytes().toList()
        val expectedHashToSign2 = "1ABE126B14B2D11CD47B27204E7F99950C7527AADF470F533021FDB1324FAD4F"
            .hexToBytes().toList()
        val expectedSignedTransaction = (
            "0100000002B6A2673BDD04D57B5560F4E46CAC3C1F974E41463568F2A11E7D3175521D9C6D000000008B4" +
                "8304502210088E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE022030E" +
                "97BD9018E9B2E918EF79E15E2747D4E00C55D69FA0B8ADFAFD07F41144F81014104E3F3BE3CE3D8284D" +
                "B3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A5456" +
                "18C174251B984DF841F49D2376F000000003F86D67DC12F3E3E7EE47E3B02D30D476823B594CBCABF112" +
                "3A8C272CC91F2AE490000008A4730440220337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744" +
                "A696AD26ECC7802200157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC58301410" +
                "4E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51" +
                "C3356A8681A545618C174251B984DF841F49D2376F00000000010010A5D4E80000001600146C607048C17BE" +
                "C51D81B4B4E23801434DCBAE0D300000000"
            ).hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData, dustValue) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() })
            .containsExactly(expectedHashToSign1, expectedHashToSign2)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}