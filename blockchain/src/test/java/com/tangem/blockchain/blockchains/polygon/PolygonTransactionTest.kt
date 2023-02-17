package com.tangem.blockchain.blockchains.polygon

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.kethereum.DEFAULT_GAS_LIMIT

class PolygonTransactionTest {

    val blockchain = Blockchain.Polygon

    @Test
    fun buildCorrectCoinTransaction() {
        // arrange
        val walletPublicKey = "04332F99A76D0ABB06356945CAF02C23B25297D05A2557B0968904792EEB1C88B8C70BCD72258F540C8B76BE1C51C9BC24DC06948758001C5BF17016336652D336"
            .hexToBytes()
        val signature = "BE3E2E3BDDF118DA63522EFE218F1CDE7D4657974D6FAFC6FF8D7CD3E72ACE8868409168421B4DE78F5FCE10494AF215028386A57678C81B06A772865431C48D"
            .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()

        val walletAddress = EthereumAddressService().makeAddress(walletPublicKey)
        val transactionBuilder = EthereumTransactionBuilder(walletPublicKey, blockchain)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Amount(amountToSend, feeValue)
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee
        )

        val expectedHashToSign = "9786CAD43696FBFF7024A2707B0A060F54F233708F0A4B4003A42D20A536B39D"
            .hexToBytes()
        val expectedSignedTransaction = "F86E0F856EDF2A079E825208947655B9B19FFAB8B897F836857DAE22A1E7F8D73588016345785D8A000080820135A0BE3E2E3BDDF118DA63522EFE218F1CDE7D4657974D6FAFC6FF8D7CD3E72ACE88A068409168421B4DE78F5FCE10494AF215028386A57678C81B06A772865431C48D"
            .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildToSign(transactionData, nonce, DEFAULT_GAS_LIMIT)
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
        val signature = "1D0FD2B5E7501533D2D3831D6CDB317BFAE015C85E4A06C6AC7966807487BBD02532612650BC3543AC5D592D13D37BD1F3677F58A3D605C6CFE94D95761BB429"
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

        val walletAddress = EthereumAddressService().makeAddress(walletPublicKey)
        val transactionBuilder = EthereumTransactionBuilder(walletPublicKey, blockchain)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Token(token))
        val fee = Amount(feeValue, blockchain, AmountType.Coin)
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
        )

        val expectedHashToSign = "3964E9A149904C6A84E06396968E7C0448937C9DEE270AAC9B6622BA7B6CB246"
            .hexToBytes()
        val expectedSignedTransaction = "F8AB0F856EDF2A079E82520894A0B86991C6218B36C1D19D4A2E9EB0CE3606EB4880B844A9059CBB0000000000000000000000007655B9B19FFAB8B897F836857DAE22A1E7F8D735000000000000000000000000000000000000000000000000016345785D8A0000820136A01D0FD2B5E7501533D2D3831D6CDB317BFAE015C85E4A06C6AC7966807487BBD0A02532612650BC3543AC5D592D13D37BD1F3677F58A3D605C6CFE94D95761BB429"
            .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildToSign(transactionData, nonce, DEFAULT_GAS_LIMIT)
        val signedTransaction = transactionBuilder.buildToSend(signature, transactionToSign!!)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}