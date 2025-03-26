package com.tangem.blockchain.blockchains.polygon

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import org.junit.Before
import org.junit.Test
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.DEFAULT_GAS_PRICE

class PolygonTransactionTest {

    private val blockchain = Blockchain.Polygon

    private val walletPublicKey = (
        "04332F99A76D0ABB06356945CAF02C23B25297D05A2557B0968904792EEB1C88B8C70BCD72258F540C8B76BE1C51C9BC24DC069" +
            "48758001C5BF17016336652D336"
        ).hexToBytes()

    private val transactionBuilder by lazy {
        EthereumTransactionBuilder.create(
            wallet = Wallet(
                blockchain = blockchain,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
                tokens = setOf(),
            ),
        )
    }

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Before
    fun setup() {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(),
        )
    }

    @Test
    fun buildCorrectCoinTransaction() {
        // arrange
        val signature = (
            "BE3E2E3BDDF118DA63522EFE218F1CDE7D4657974D6FAFC6FF8D7CD3E72ACE8868409168421B4DE78F5FCE10494AF215028386A" +
                "57678C81B06A772865431C48D"
            ).hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()

        val walletAddress = EthereumAddressService().makeAddress(walletPublicKey)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Ethereum.Legacy(
            amount = Amount(amountToSend, feeValue),
            gasLimit = DEFAULT_GAS_LIMIT,
            gasPrice = DEFAULT_GAS_PRICE,
        )
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(nonce = nonce),
        )

        val expectedHashToSign = "8EC5BBC80DA9914FA792AD8B046FE79284251EE408701E286BFF65FC7230945C".hexToBytes()
        val expectedSignedTransaction = (
            "F86E0F856EDF2A079E825208947655B9B19FFAB8B897F836857DAE22A1E7F8D73588016345785D8A000080820135A0BE3E2E3BD" +
                "DF118DA63522EFE218F1CDE7D4657974D6FAFC6FF8D7CD3E72ACE88A068409168421B4DE78F5FCE10494AF215028386A576" +
                "78C81B06A772865431C48D"
            ).hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectTokenTransaction() {
        // arrange
        val signature = (
            "1D0FD2B5E7501533D2D3831D6CDB317BFAE015C85E4A06C6AC7966807487BBD02532612650BC3543AC5D592D13D37BD1F3677F5" +
                "8A3D605C6CFE94D95761BB429"
            ).hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()
        val contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        val token = Token(
            symbol = "USDC",
            contractAddress = contractAddress,
            decimals = 6,
        )

        val walletAddress = EthereumAddressService().makeAddress(walletPublicKey)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Token(token))
        val fee = Fee.Ethereum.Legacy(
            amount = Amount(feeValue, blockchain, AmountType.Coin),
            gasLimit = DEFAULT_GAS_LIMIT,
            gasPrice = DEFAULT_GAS_PRICE,
        )
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(nonce = nonce),
        )

        val expectedHashToSign = "B043051966A599EE2A1B275491E2BC5D58C01F3C7925353F24DB90F7D117F12F".hexToBytes()
        val expectedSignedTransaction = (
            "F8AB0F856EDF2A079E82520894A0B86991C6218B36C1D19D4A2E9EB0CE3606EB4880B844A9059CBB00000000000000000000000" +
                "07655B9B19FFAB8B897F836857DAE22A1E7F8D735000000000000000000000000000000000000000000000000016345785D" +
                "8A0000820136A01D0FD2B5E7501533D2D3831D6CDB317BFAE015C85E4A06C6AC7966807487BBD0A02532612650BC3543AC5" +
                "D592D13D37BD1F3677F58A3D605C6CFE94D95761BB429"
            ).hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}