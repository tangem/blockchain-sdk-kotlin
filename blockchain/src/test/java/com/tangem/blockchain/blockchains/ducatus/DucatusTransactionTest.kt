package com.tangem.blockchain.blockchains.ducatus

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionTest
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class DucatusTransactionTest {

    private val blockchain = Blockchain.Ducatus
    private val networkParameters = DucatusMainNetParams()

    @Test
    fun buildCorrectTransaction() {
        // arrange
        val walletPublicKey = "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376F"
                .hexToBytes()
        val signature = "88E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B2E918EF79E15E2747D4E00C55D69FA0B8ADFAFD07F41144F81337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "M6tZXSEVGErPo8TnmpPv8Zvp69uSmLwJmF"

        val address = BitcoinAddressService(blockchain)
            .makeAddress(publicKey = Wallet.PublicKey(walletPublicKey, null), addressType = AddressType.Legacy,
                curve = EllipticCurve.Secp256k1)

        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, listOf(address) )
        transactionBuilder.unspentOutputs =
                BitcoinTransactionTest.prepareTwoUnspentOutputs(listOf(address.value), networkParameters)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = address.value,
                destinationAddress = destinationAddress,
                amount = amountToSend,
                fee = fee
        )

        val expectedHashToSign1 = "F1C2ECA8E0C2AE8D04F5C58BD5FFC74DFD6171A2D33174CB93770F170BBBB603"
                .hexToBytes().toList()
        val expectedHashToSign2 = "C37B01690AF7C88C6813644D49D63D5EEBFA0512CD143EC10B5E538C4C578387"
                .hexToBytes().toList()
        val expectedSignedTransaction = "0100000002B6A2673BDD04D57B5560F4E46CAC3C1F974E41463568F2A11E7D3175521D9C6D000000008B48304502210088E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE022030E97BD9018E9B2E918EF79E15E2747D4E00C55D69FA0B8ADFAFD07F41144F81014104E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376FFFFFFFFF3F86D67DC12F3E3E7EE47E3B02D30D476823B594CBCABF1123A8C272CC91F2AE490000008A4730440220337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC7802200157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583014104E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376FFFFFFFFF0280969800000000001976A914F4EAE70648A8B0DFC935B73C7C021CAFDCBE32E788AC80790CD4E80000001976A914C5C53741303B67E7FE2EA62CB5730B3DD32D75FF88AC00000000"
                .hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() })
                .containsExactly(expectedHashToSign1, expectedHashToSign2)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}