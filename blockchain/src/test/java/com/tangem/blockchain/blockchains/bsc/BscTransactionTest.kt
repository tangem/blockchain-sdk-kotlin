package com.tangem.blockchain.blockchains.bsc

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.DEFAULT_GAS_PRICE

class BscTransactionTest {

    private val blockchain = Blockchain.BSC

    @Test
    fun buildCorrectCoinTransaction() {
        // arrange
        val walletPublicKey = "04332F99A76D0ABB06356945CAF02C23B25297D05A2557B0968904792EEB1C88B8C70BCD72258F540C8B76BE1C51C9BC24DC06948758001C5BF17016336652D336"
            .hexToBytes()
        val signature = "BABE797847D1BD14A9A8BF8704D9BB10456C27A39E714A0FB40B668EEF8A79F72D65F9C65735DC38EB5D8DCB26755DDA794FC0F9156135B97C3A3993C75BAFDE"
            .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()

        val walletAddress = EthereumAddressService().makeAddressWithDefaultType(walletPublicKey)
        val transactionBuilder = EthereumTransactionBuilder(walletPublicKey, blockchain)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Ethereum(Amount(amountToSend, feeValue), DEFAULT_GAS_LIMIT, DEFAULT_GAS_PRICE)
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee
        )

        val expectedHashToSign = "166A2BF5E57A4732331F876327803E1D35559D8A4F7BEBF6356EA30BC52F0258"
            .hexToBytes()
        val expectedSignedTransaction = "F86D0F856EDF2A079E825208947655B9B19FFAB8B897F836857DAE22A1E7F8D73588016345785D8A0000808194A0BABE797847D1BD14A9A8BF8704D9BB10456C27A39E714A0FB40B668EEF8A79F7A02D65F9C65735DC38EB5D8DCB26755DDA794FC0F9156135B97C3A3993C75BAFDE"
            .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildToSign(transactionData, nonce)
        val signedTransaction = transactionBuilder.buildToSend(signature, transactionToSign!!)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectTokenTransaction() {
        // arrange
        val walletPublicKey = "04332F99A76D0ABB06356945CAF02C23B25297D05A2557B0968904792EEB1C88B8C70BCD72258F540C8B76BE1C51C9BC24DC06948758001C5BF17016336652D336"
            .hexToBytes()
        val signature = "F87F35C5F5EEAD78722315FC7F4CF303BB985479F082550DFC859A45BC39693E5254F458E65EC802EDD725A312AE2A967D6B761CB0708DFF0EC5F08901033CA4"
            .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()
        val contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        val token = Token(
            symbol = "USDC",
            contractAddress = contractAddress,
            decimals = 6
        )

        val walletAddress = EthereumAddressService().makeAddressWithDefaultType(walletPublicKey)
        val transactionBuilder = EthereumTransactionBuilder(walletPublicKey, blockchain)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Token(token))
        val fee = Fee.Ethereum(Amount(feeValue, blockchain, AmountType.Coin), DEFAULT_GAS_LIMIT, DEFAULT_GAS_PRICE)
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
        )

        val expectedHashToSign = "95DB218B4288E60DA2807FC42EFBB2BFEF0A19607EC1D701AF54704DC5A253F6"
            .hexToBytes()
        val expectedSignedTransaction = "F8AA0F856EDF2A079E82520894A0B86991C6218B36C1D19D4A2E9EB0CE3606EB4880B844A9059CBB0000000000000000000000007655B9B19FFAB8B897F836857DAE22A1E7F8D735000000000000000000000000000000000000000000000000016345785D8A00008193A0F87F35C5F5EEAD78722315FC7F4CF303BB985479F082550DFC859A45BC39693EA05254F458E65EC802EDD725A312AE2A967D6B761CB0708DFF0EC5F08901033CA4"
            .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildToSign(transactionData, nonce)
        val signedTransaction = transactionBuilder.buildToSend(signature, transactionToSign!!)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}