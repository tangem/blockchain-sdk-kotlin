package com.tangem.blockchain.blockchains.stellar

import com.google.common.truth.Truth
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Calendar

class StellarTransactionTest {

    val blockchain = Blockchain.Stellar

    @Test
    fun buildCorrectCoinTransaction() {
        // arrange
        val walletPublicKey = "64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C".hexToBytes()
        val signature = (
            "C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD3D31BEB446589EDC761493C369" +
                "CDA6B13AC09D122C58C7F5903832678371A96D"
            ).hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "GAI3GJ2Q3B35AOZJ36C4ANE3HSS4NK7WI6DNO4ZSHRAX6NG7BMX6VJER"
        val sequence = 123118694988529223L
        val instant = 1612201818L

        val walletAddress = StellarAddressService().makeAddress(walletPublicKey)
        val calendar = Calendar.Builder().setInstant(instant).build()
        val transactionBuilder = StellarTransactionBuilder(StellarNetworkServiceMock(), walletPublicKey)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            date = calendar,
        )

        val expectedHashToSign = "43D14A19E4FB9D21F461F9A2EC558D843E2E4CEEBEE8EB48740CFB496C38BC23".hexToBytes()
        val expectedSignedTransaction = "AAAAAgAAAABk32doDyFn4aCFCD/jCFVh5r71qh/BZXhf+uYmRwbbjAABhqABtWfNAAA+SAAAAAE" +
            "AAAAAAAAAAAAAAAAAGJohAAAAAAAAAAEAAAAAAAAAAQAAAAARsydQ2HfQOynfhcA0mzylxqv2R4bXczI8QX803wsv6gAAAAAAAAAAAA" +
            "9CQAAAAAAAAAABRwbbjAAAAEDA+8MlVELK5YL9w8+PQxqqsLidHQ372ucf7kT5nkwRvT0xvrRGWJ7cdhSTw2nNprE6wJ0SLFjH9ZA4M" +
            "meDcalt"

        // act
        val buildToSignResult = runBlocking {
            transactionBuilder.buildToSign(transactionData, sequence) as Result.Success
        }
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectTokenTransaction() {
        // arrange
        val walletPublicKey = "64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C".hexToBytes()
        val signature = (
            "C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD3D31BEB446589EDC761493C369CDA6B13AC09D1" +
                "22C58C7F5903832678371A96D"
            ).hexToBytes()
        val sendValue = "3".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "GAI3GJ2Q3B35AOZJ36C4ANE3HSS4NK7WI6DNO4ZSHRAX6NG7BMX6VJER"
        val sequence = 123118694988529223L
        val instant = 1612201818L
        val token = Token(
            symbol = "MYNT",
            contractAddress = "GAHSHLZHWC3BGDDZ3JGUYXOHOQT4PCPL5WBQPGBXFB4OD3LG2MTOSRXD",
            decimals = 0,
        )

        val walletAddress = StellarAddressService().makeAddress(walletPublicKey)
        val calendar = Calendar.Builder().setInstant(instant).build()
        val transactionBuilder = StellarTransactionBuilder(StellarNetworkServiceMock(), walletPublicKey)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Token(token))
        val fee = Fee.Common(Amount(feeValue, blockchain, AmountType.Coin))
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            date = calendar,
        )

        val expectedHashToSign = "89632F5A2FD4A6D94859F3A0C53468489EFE8BCAF0AB3A89847775C2D7610749"
            .hexToBytes()
        val expectedSignedTransaction = "AAAAAgAAAABk32doDyFn4aCFCD/jCFVh5r71qh/BZXhf+uYmRwbbjAABhqABtWfNAAA+SAAAAAE" +
            "AAAAAAAAAAAAAAAAAGJohAAAAAAAAAAEAAAAAAAAAAQAAAAARsydQ2HfQOynfhcA0mzylxqv2R4bXczI8QX803wsv6gAAAAFYTE0AAA" +
            "AAAA8jryewthMMedpNTF3HdCfHievtgweYNyh44e1m0ybpAAAAAAHJw4AAAAAAAAAAAUcG24wAAABAwPvDJVRCyuWC/cPPj0MaqrC4n" +
            "R0N+9rnH+5E+Z5MEb09Mb60Rlie3HYUk8NpzaaxOsCdEixYx/WQODJng3GpbQ=="
        // act
        val buildToSignResult = runBlocking {
            transactionBuilder.buildToSign(transactionData, sequence) as Result.Success
        }
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}
