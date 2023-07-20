package com.tangem.blockchain.blockchains.cardano

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.wrapInObject
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class CardanoTransactionTest {

    val blockchain = Blockchain.CardanoShelley

    val addressService = CardanoAddressService(blockchain)

    @Test
    fun buildCorrectTransaction() {
        // arrange
        val walletPublicKey = "64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C"
                .hexToBytes()
        val signature = "E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val byronDestinationAddress = "Ae2tdPwUPEYyDf8J4KQNr1ZPw26iyn9JU9dHWTAxNEaNbi8VDNDTBmjQuXj"

        val byronAddress = addressService.makeAddress(walletPublicKey.wrapInObject(), AddressType.Legacy)
        val shelleyAddress = addressService.makeAddress(walletPublicKey.wrapInObject(), AddressType.Default)

        val transactionBuilder = CardanoTransactionBuilder(walletPublicKey)
        transactionBuilder.unspentOutputs = prepareTwoUnspentOutputs(listOf(byronAddress.value, shelleyAddress.value))

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = shelleyAddress.value,
                destinationAddress = byronDestinationAddress,
                amount = amountToSend,
                fee = fee
        )

        val expectedHashToSign = "489DA8196FE9C0F5961D0CFA692DDC412C684290340B32D29ADE0A35471AA499"
                .hexToBytes()
        val expectedSignedTransaction = "83A40082825820CEC3E29C82B8A8180959145181ED357A1FED0D89079D376DCC6F79CCBAF5F5E3008258200D19A16378665928B997FE8D0A503D587400551090CC17570117CC97A0DB1CF71849018282582B82D818582183581C182C4606330E769D4C9CA3008B0FB658E9C8CD40DDD497AA6CA1D214A0001A90C1457E1A000186A082581D61FB935AE2829A5F59E8ACACC50206066F211CD33DFB58F10AEC3E49BD1B000000025437FD1002192710031A0B532B80A2008182582064DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C5880E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583028184582064DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C5880E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC5835820000000000000000000000000000000000000000000000000000000000000000041A0F6"
                .hexToBytes()

        // act
        val hashToSign = transactionBuilder.buildToSign(transactionData)
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(hashToSign).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    private fun prepareTwoUnspentOutputs(addresses: List<String>): List<CardanoUnspentOutput> {
        if (addresses.isEmpty()) throw Exception("Address is needed to prepare utxo")

        val utxo1 = CardanoUnspentOutput(
                amount = 10000000000L,
                outputIndex = 0,
                transactionHash = "cec3e29c82b8a8180959145181ed357a1fed0d89079d376dcc6f79ccbaf5f5e3"
                        .hexToBytes(),
                address = addresses[0]
        )
        val utxo2 = CardanoUnspentOutput(
                amount = 3000000L,
                outputIndex = 73,
                transactionHash = "0d19a16378665928b997fe8d0a503d587400551090cc17570117cc97a0db1cf7"
                        .hexToBytes(),
                address = addresses.getOrNull(1) ?: addresses[0]
        )
        return listOf(utxo1, utxo2)
    }
}