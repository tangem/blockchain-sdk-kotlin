package com.tangem.blockchain.blockchains.xrp

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.runBlocking
import org.junit.Test

class XrpTransactionTest {

    private val blockchain = Blockchain.XRP

    @Test
    fun buildCorrectTransactionEdKeyXAddress() {
        // arrange
        val walletPublicKey = "64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C"
                .hexToBytes()
        val signature = "C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD3D31BEB446589EDC761493C369CDA6B13AC09D122C58C7F5903832678371A96D"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val sequence = 1406L
        val destinationAddress = "X7gd8rw2UJP3HSS9oxkDc3cYVgpJy4cR9R5TEjF9XoZYJ1p"

        val walletAddress = XrpAddressService().makeAddressWithDefaultType(walletPublicKey)
        val transactionBuilder = XrpTransactionBuilder(XrpNetworkProviderMock(), walletPublicKey)
        transactionBuilder.sequence = sequence

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = walletAddress,
                destinationAddress = destinationAddress,
                amount = amountToSend,
                fee = fee
        )

        val expectedDataToSign = "535458001200002280000000240000057E2E000003096140000000000186A06840000000000027107321ED64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C8114C037558FCDDE12BE53E8C5CA5698D56DF89CC31A83142C0CB742AD230DCBC12703213B6848F7E990E188"
                .hexToBytes()
        val expectedSignedTransaction = "1200002280000000240000057E2E000003096140000000000186A06840000000000027107321ED64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C7440C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD3D31BEB446589EDC761493C369CDA6B13AC09D122C58C7F5903832678371A96D8114C037558FCDDE12BE53E8C5CA5698D56DF89CC31A83142C0CB742AD230DCBC12703213B6848F7E990E188"

        // act
        val buildToSignResult = runBlocking {
            transactionBuilder.buildToSign(transactionData) as Result.Success
        }
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data).isEqualTo(expectedDataToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectTransactionSecpKeyRAddressWithTag() {
        // arrange
        val walletPublicKey = "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
                .hexToBytes()
        val signature = "C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD3D31BEB446589EDC761493C369CDA6B13AC09D122C58C7F5903832678371A96D"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val sequence = 1406L
        val destinationAddress = "rnruxxLTbJUMNtFNBJ7X2xSiy1KE7ajUuH"

        val walletAddress = XrpAddressService().makeAddressWithDefaultType(walletPublicKey)
        val transactionBuilder = XrpTransactionBuilder(XrpNetworkProviderMock(), walletPublicKey)
        transactionBuilder.sequence = sequence

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = walletAddress,
                destinationAddress = destinationAddress,
                amount = amountToSend,
                fee = fee,
                extras = XrpTransactionBuilder.XrpTransactionExtras(12345)
        )

        val expectedHashToSign = "5330232912E58908B10EF4A26ECC55347F11FA19940F20DF5BAA6FEF2963237B"
                .hexToBytes()
        val expectedSignedTransaction = "1200002280000000240000057E2E000030396140000000000186A0684000000000002710732103EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126F74473045022100C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD02203D31BEB446589EDC761493C369CDA6B13AC09D122C58C7F5903832678371A96D811452D98CA1F2A0EE4420F852B8A456D8C15FE7B04883142C0CB742AD230DCBC12703213B6848F7E990E188"

        // act
        val buildToSignResult = runBlocking {
            transactionBuilder.buildToSign(transactionData) as Result.Success
        }
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}