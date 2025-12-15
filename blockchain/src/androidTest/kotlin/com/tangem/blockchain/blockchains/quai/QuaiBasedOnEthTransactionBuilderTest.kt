package com.tangem.blockchain.blockchains.quai

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Before
import org.junit.Test

class QuaiBasedOnEthTransactionBuilderTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Before
    fun setup() {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = false,
            ),
        )
    }

    @Test
    fun buildCorrectQuaiCoinTransaction() {
        val blockchain = Blockchain.Quai
        val walletPublicKey =
            "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
                .hexToBytes()
        val transactionBuilder = EthereumTransactionBuilder.create(
            wallet = Wallet(
                blockchain = blockchain,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
                tokens = setOf(),
            ),
        )

        val sendValue = "1".toBigDecimal()
        val destinationAddress = "0x0012345678901234567890123456789012345678"
        val nonce = 1.toBigInteger()
        val walletAddress = "0x0055555555555555555555555555555555555555"

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Ethereum.Legacy(
            amount = Amount("0.01".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "21000".toBigInteger(),
            gasPrice = "1000000000".toBigInteger(),
        )
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(nonce = nonce),
        )

        val transactionToSign = transactionBuilder.buildForSign(transactionData)

        Truth.assertThat(transactionToSign.hash).isNotNull()
        Truth.assertThat(transactionToSign.hash.size).isEqualTo(32)

        val expectedHashHex = "4A09FA62ADFF333E313FB4149228C46EEED459B2A1C046EE34C49A5577049668"
        val actualHashHex = transactionToSign.hash.toHexString()

        Truth.assertThat(actualHashHex).isNotEmpty()
        Truth.assertThat(actualHashHex.length).isEqualTo(64)
        Truth.assertThat(actualHashHex).isEqualTo(expectedHashHex)
    }

    @Test
    fun verifyQuaiProtobufStructure() {
        val blockchain = Blockchain.Quai
        val walletPublicKey =
            "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
                .hexToBytes()
        val wallet = Wallet(
            blockchain = blockchain,
            addresses = setOf(),
            publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
            tokens = setOf(),
        )

        val transactionBuilder = EthereumTransactionBuilder.create(wallet)

        val destinationAddress = "0x0012345678901234567890123456789012345678"
        val sourceAddress = "0x0055555555555555555555555555555555555555"
        val value = Amount("2".toBigDecimal(), blockchain, AmountType.Coin)
        val nonce = 5.toBigInteger()

        val fee = Fee.Ethereum.Legacy(
            amount = Amount("0.02".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "21000".toBigInteger(),
            gasPrice = "2000000000".toBigInteger(),
        )

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            amount = value,
            fee = fee,
            extras = EthereumTransactionExtras(nonce = nonce),
        )

        val transactionToSign = transactionBuilder.buildForSign(transactionData)

        Truth.assertThat(transactionToSign.hash).isNotNull()
        Truth.assertThat(transactionToSign.hash.size).isEqualTo(32)

        val expectedHashHex = "6CB3775BB6F4005CEAAFE90E0C1F2AAFC85DA170F94F6740EF3E7D5F28422993"
        val actualHashHex = transactionToSign.hash.toHexString()

        Truth.assertThat(actualHashHex).isNotEmpty()
        Truth.assertThat(actualHashHex.length).isEqualTo(64)
        Truth.assertThat(actualHashHex).isEqualTo(expectedHashHex)
    }

    @Test
    fun verifyQuaiCyprus1AddressValidation() {
        val validCyprus1Addresses = listOf(
            "0x0012345678901234567890123456789012345678",
            "0x0055b9b19ffab8b897f836857dae22a1e7f8d735",
            "0x0004d59c8583e37426b37d1d7394b6008a987c67",
        )

        val invalidCyprus1Addresses = listOf(
            "0x7655b9b19ffab8b897f836857dae22a1e7f8d735",
            "0xb1123efF798183B7Cb32F62607D3D39E950d9cc3",
            "0x0090e4d59c8583e37426b37d1d7394b6008a987c67",
        )

        val addressService = QuaiAddressService()

        validCyprus1Addresses.forEach { address ->
            Truth.assertThat(addressService.validate(address)).isTrue()
        }

        invalidCyprus1Addresses.forEach { address ->
            Truth.assertThat(addressService.validate(address)).isFalse()
        }
    }
}