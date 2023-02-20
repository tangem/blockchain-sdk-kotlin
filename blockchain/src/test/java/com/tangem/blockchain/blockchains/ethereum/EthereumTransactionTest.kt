package com.tangem.blockchain.blockchains.ethereum

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.kethereum.DEFAULT_GAS_LIMIT

class EthereumTransactionTest {

    val blockchain = Blockchain.Ethereum

    @Test
    fun buildCorrectCoinTransaction() {
        // arrange
        val walletPublicKey = "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
                .hexToBytes()
        val signature = "B945398FB90158761F6D61789B594D042F0F490F9656FBFFAE8F18B49D5F30054F43EE43CCAB2703F0E2E4E61D99CF3D4A875CD759569787CF0AED02415434C6"
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

        val expectedHashToSign = "BDBECF64B443F82D1F9FDA3F2D6BA69AF6D82029B8271339B7E775613AE57761"
                .hexToBytes()
        val expectedSignedTransaction = "F86C0F856EDF2A079E825208947655B9B19FFAB8B897F836857DAE22A1E7F8D73588016345785D8A00008025A0B945398FB90158761F6D61789B594D042F0F490F9656FBFFAE8F18B49D5F3005A04F43EE43CCAB2703F0E2E4E61D99CF3D4A875CD759569787CF0AED02415434C6"
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
        val walletPublicKey = "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
                .hexToBytes()
        val signature = "F408C40F8D8B4A40E35502355C87FBBF218EC9ECB036D42DAA6211EAD4498A6FBC800E82CB2CC0FAB1D68FD3F8E895EC3E0DCB5A05342F5153210142E4224D4C"
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

        val expectedHashToSign = "2F47B058A0C4A91EC6E26372FA926ACB899235D7A639565B4FC82C7A9356D6C5"
                .hexToBytes()
        val expectedSignedTransaction = "F8A90F856EDF2A079E82520894A0B86991C6218B36C1D19D4A2E9EB0CE3606EB4880B844A9059CBB0000000000000000000000007655B9B19FFAB8B897F836857DAE22A1E7F8D735000000000000000000000000000000000000000000000000016345785D8A000025A0F408C40F8D8B4A40E35502355C87FBBF218EC9ECB036D42DAA6211EAD4498A6FA0437FF17D34D33F054E29702C07176A127CA1118CAA1470EA6CB15D49EC13F3F5"
                .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildToSign(transactionData, nonce, DEFAULT_GAS_LIMIT)
        val signedTransaction = transactionBuilder.buildToSend(signature, transactionToSign!!)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}