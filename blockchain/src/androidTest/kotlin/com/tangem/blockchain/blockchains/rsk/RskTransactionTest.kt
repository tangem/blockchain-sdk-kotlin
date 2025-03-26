package com.tangem.blockchain.blockchains.rsk

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

class RskTransactionTest {

    private val blockchain = Blockchain.RSK

    private val walletPublicKey = (
        "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF88" +
            "82A3125B0EE9AE96DDDE1AE743F"
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
            "C0FBC3255442CAE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BD3D31BEB446589EDC761493C369CDA6B13AC09D1" +
                "22C58C7F5903832678371A96D"
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

        val expectedHashToSign = "B0F824BE586D3BB662DDD855B3BF1165DF0149717A749E986EDBBDCE82A86D22".hexToBytes()
        val expectedSignedTransaction = (
            "F86C0F856EDF2A079E825208947655B9B19FFAB8B897F836857DAE22A1E7F8D73588016345785D8A0000805FA0C0FBC3255442C" +
                "AE582FDC3CF8F431AAAB0B89D1D0DFBDAE71FEE44F99E4C11BDA03D31BEB446589EDC761493C369CDA6B13AC09D122C58C7" +
                "F5903832678371A96D"
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
            "3057FEA6F18CC08553F79E985646262E06C0ED50DB7583D236958AB3ADB5D71333D1A8395CD286AF65FB781A2C2461132A9F34" +
                "014CCDC68586B8F458848F4717"
            ).hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()
        val contractAddress = "0x2acc95758f8b5f583470ba265eb685a8f45fc9d5"
        val token = Token(
            symbol = "RIF",
            contractAddress = contractAddress,
            decimals = 18,
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

        val expectedHashToSign = "F3D1B4E2B5A34B0A3D34F3B34D97E35E5CF906DEF3EEABB10B4A218ACCE34DFC".hexToBytes()
        val expectedSignedTransaction = (
            "F8A90F856EDF2A079E825208942ACC95758F8B5F583470BA265EB685A8F45FC9D580B844A9059CBB00000000000000000000000" +
                "07655B9B19FFAB8B897F836857DAE22A1E7F8D735000000000000000000000000000000000000000000000000016345785D" +
                "8A000060A03057FEA6F18CC08553F79E985646262E06C0ED50DB7583D236958AB3ADB5D713A033D1A8395CD286AF65FB781" +
                "A2C2461132A9F34014CCDC68586B8F458848F4717"
            ).hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}