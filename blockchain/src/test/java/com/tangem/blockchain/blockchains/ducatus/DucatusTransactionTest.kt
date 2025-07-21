package com.tangem.blockchain.blockchains.ducatus

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

class DucatusTransactionTest {

    private val blockchain = Blockchain.Ducatus
    private val networkParameters = DucatusMainNetParams()
    private val dustValue = 0.00001.toBigDecimal()

    @Test
    fun buildCorrectTransaction() {
        // arrange
        val walletPublicKey = (
            "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A5456" +
                "18C174251B984DF841F49D2376F"
            ).hexToBytes()
        val signature = (
            "88E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B2E918EF79E15E2747D4E00C55" +
                "D69FA0B8ADFAFD07F41144F81337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44" +
                "DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
            ).hexToBytes()
        val sendValue = "10000".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "M6tZXSEVGErPo8TnmpPv8Zvp69uSmLwJmF"

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val address = addresses.find { it.type == AddressType.Default }!!.value
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

        val expectedHashToSign1 = "D0AF7E56584AE0B1D5D2CDCA10DED6427D66C0A41D5AC1310F01F350383F56CA"
            .hexToBytes().toList()
        val expectedHashToSign2 = "710A4D2D9C2E70F3975B64D06E6EEE7FD88F08D7379E1B9F8D6F5C6D07CCCF4A"
            .hexToBytes().toList()
        val expectedSignedTransaction = (
            "0100000002B6A2673BDD04D57B5560F4E46CAC3C1F974E41463568F2A11E7D3175521D9C6D000000008" +
                "B48304502210088E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE02" +
                "2030E97BD9018E9B2E918EF79E15E2747D4E00C55D69FA0B8ADFAFD07F41144F81014104E3F3BE3C" +
                "E3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C33" +
                "56A8681A545618C174251B984DF841F49D2376F000000003F86D67DC12F3E3E7EE47E3B02D30D4768" +
                "23B594CBCABF1123A8C272CC91F2AE490000008A4730440220337D7F3BD0798D66FDCE04B07C30984424" +
                "B13B98BB2C3645744A696AD26ECC7802200157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2" +
                "D33A877FC583014104E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E10" +
                "3F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376F00000000010010A5D4E80000" +
                "001976A914F4EAE70648A8B0DFC935B73C7C021CAFDCBE32E788AC00000000"
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