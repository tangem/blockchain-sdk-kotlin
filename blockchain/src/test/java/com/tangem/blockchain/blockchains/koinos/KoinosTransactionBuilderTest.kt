package com.tangem.blockchain.blockchains.koinos

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.koinos.models.KoinosAccountNonce
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosProtocol
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class KoinosTransactionBuilderTest {

    private val transactionBuilder = KoinosTransactionBuilder(isTestnet = false)
    private val transactionBuilderTestnet = KoinosTransactionBuilder(isTestnet = true)

    private val transactionDataForTest = TransactionData(
        amount = Amount(
            value = 12.toBigDecimal().setScale(Blockchain.Koinos.decimals()),
            blockchain = Blockchain.Koinos,
            type = AmountType.Coin,
        ),
        fee = null,
        sourceAddress = "1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8Tp",
        destinationAddress = "1AWFa3VVwa2C54EU18NUDDYxjsPDwxKAuB",
        extras = KoinosTransactionExtras(
            manaLimit = 5.toBigDecimal().setScale(Blockchain.Koinos.decimals()),
        ),
    )
    private val currentNonceForTest = KoinosAccountNonce(10.toBigInteger())

    private val expectedTransactionToSign = KoinosProtocol.Transaction(
        header = KoinosProtocol.TransactionHeader(
            chainId = "EiBZK_GGVP0H_fXVAM3j6EAuz3-B-l3ejxRSewi7qIBfSA==",
            rcLimit = 500000000,
            nonce = "KAs=",
            operationMerkleRoot = "EiBd86ETLP-Tmmq-Oj6wxfe1o2KzRGf_9LV-9O3_9Qmu8w==",
            payer = "1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8Tp",
            payee = null,
        ),
        id = "0x12201042aeee64fcc89921d0b5f9bdd6c9bff3e9c089d3579c74882fe0f018acd608",
        operations = listOf(
            KoinosProtocol.Operation(
                callContract = KoinosProtocol.CallContractOperation(
                    contractIdBase58 = "15DJN4a8SgrbGhhGksSBASiSYjGnMU8dGL",
                    entryPoint = 670398154,
                    argsBase64 = "ChkAaMW2_tO2QuoaSAiMXztphDRhY2m4f6efEhkAaEFbbHucCFnoEOh3RgGrOZ38TNTI9xMWGICYmrwE",
                ),
            ),
        ),
        signatures = emptyList(),
    )
    private val expectedHashToSign = "1042AEEE64FCC89921D0B5F9BDD6C9BFF3E9C089D3579C74882FE0F018ACD608".hexToBytes()

    private val expectedTransactionToSignTestnet = KoinosProtocol.Transaction(
        header = KoinosProtocol.TransactionHeader(
            chainId = "EiBncD4pKRIQWco_WRqo5Q-xnXR7JuO3PtZv983mKdKHSQ==",
            rcLimit = 500000000,
            nonce = "KAs=",
            operationMerkleRoot = "EiCjvMCnYVk5GqAaz7D2e8LCbaJ6448pJMXS4LI_EjtW4Q==",
            payer = "1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8Tp",
            payee = null,
        ),
        id = "0x1220f90ab33fcd0fa5896bb56352875eb49ac984cfd347467a50fe7a28686b11bb45",
        operations = listOf(
            KoinosProtocol.Operation(
                callContract = KoinosProtocol.CallContractOperation(
                    contractIdBase58 = "1FaSvLjQJsCJKq5ybmGsMMQs8RQYyVv8ju",
                    entryPoint = 670398154,
                    argsBase64 = "ChkAaMW2_tO2QuoaSAiMXztphDRhY2m4f6efEhkAaEFbbHucCFnoEOh3RgGrOZ38TNTI9xMWGICYmrwE",
                ),
            ),
        ),
        signatures = emptyList(),
    )
    private val expectedHashToSignTestnet =
        "F90AB33FCD0FA5896BB56352875EB49AC984CFD347467A50FE7A28686B11BB45".hexToBytes()

    @Test
    fun buildToSign() {
        val transactionToSignResult = transactionBuilder.buildToSign(
            transactionData = transactionDataForTest,
            currentNonce = currentNonceForTest,
        )
        val (transactionToSign, hashToSign) = (transactionToSignResult as Result.Success).data

        Truth.assertThat(hashToSign)
            .isEqualTo(expectedHashToSign)
        Truth.assertThat(transactionToSign)
            .isEqualTo(expectedTransactionToSign)
    }

    @Test
    fun buildToSignTestnet() {
        val transactionToSignResult = transactionBuilderTestnet.buildToSign(
            transactionData = transactionDataForTest,
            currentNonce = currentNonceForTest,
        )
        val (transactionToSign, hashToSign) = (transactionToSignResult as Result.Success).data

        Truth.assertThat(hashToSign)
            .isEqualTo(expectedHashToSignTestnet)
        Truth.assertThat(transactionToSign)
            .isEqualTo(expectedTransactionToSignTestnet)
    }

    @Test
    fun buildToSend() {
        val expectedSignature =
            "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

        val signedTransaction = transactionBuilder.buildToSend(
            expectedTransactionToSign,
            ByteArray(64) { 0x00.toByte() },
        )
        val signedTransactionTestnet = transactionBuilderTestnet.buildToSend(
            expectedTransactionToSign,
            ByteArray(64) { 0x00.toByte() },
        )

        Truth.assertThat(signedTransaction.signatures.size)
            .isEqualTo(1)
        Truth.assertThat(signedTransactionTestnet.signatures.size)
            .isEqualTo(1)
        Truth.assertThat(signedTransaction.signatures[0])
            .isEqualTo(expectedSignature)
        Truth.assertThat(signedTransactionTestnet.signatures[0])
            .isEqualTo(expectedSignature)
    }
}