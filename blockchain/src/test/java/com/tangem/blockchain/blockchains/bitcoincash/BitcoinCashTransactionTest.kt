package com.tangem.blockchain.blockchains.bitcoincash

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionTest
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams
import org.junit.Test

class BitcoinCashTransactionTest {

    private val blockchain = Blockchain.BitcoinCash
    private val networkParameters = MainNetParams()
    private val addressService = BitcoinCashAddressService(Blockchain.BitcoinCash)

    @Test
    fun buildCorrectTransaction() {
        // arrange
        val walletPublicKey = (
            "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF88" +
                "82A3125B0EE9AE96DDDE1AE743F"
            ).hexToBytes()
        val signature = (
            "E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C" +
                "30984424B13344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC780157EA9D44" +
                "DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
            ).hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p"

        val address = addressService.makeAddress(walletPublicKey)
        val legacyAddress = LegacyAddress
            .fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(address))
            .toBase58()

        val transactionBuilder = BitcoinCashTransactionBuilder(walletPublicKey, blockchain)
        transactionBuilder.unspentOutputs = BitcoinTransactionTest
            .prepareTwoUnspentOutputs(listOf(legacyAddress), networkParameters)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
            sourceAddress = address,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
        )

        val expectedHashToSign1 = "F430E42F4342382C32A043F0A3130E6CC1DF6378A2D6422794F8A21B2086C370"
            .hexToBytes().toList()
        val expectedHashToSign2 = "A9DD66EB35FACC8ECDACC48606CE988ED7B870E49238557C978FE6001C7905C5"
            .hexToBytes().toList()
        val expectedSignedTransaction = (
            "0100000002B6A2673BDD04D57B5560F4E46CAC3C1F974E41463568F2A11E7D3175521D9C6D000000006B483045022100E2747D4" +
                "E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B02202E918EF79E151337D7F3BD0798D66FDCE04B07" +
                "C30984424B13344F0A7CC40165412103EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FFFF" +
                "FFFFF3F86D67DC12F3E3E7EE47E3B02D30D476823B594CBCABF1123A8C272CC91F2AE490000006A47304402204BF71C43DF" +
                "96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC7802200157EA9D44DC41D0BCB420175A5D3F543079F4263" +
                "AA2DBDE0EE2D33A877FC583412103EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FFFFFFF" +
                "FF0280969800000000001976A914F1C075A01882AE0972F95D3A4177C86C852B7D9188AC80790CD4E80000001976A91452D" +
                "98CA1F2A0EE4420F852B8A456D8C15FE7B04888AC00000000"
            ).hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() })
            .containsExactly(expectedHashToSign1, expectedHashToSign2)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}
