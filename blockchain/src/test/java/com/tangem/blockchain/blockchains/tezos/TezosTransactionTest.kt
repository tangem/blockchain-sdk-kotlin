package com.tangem.blockchain.blockchains.tezos

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.tezos.network.TezosOperationContent
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.blockchain.wrapInObject
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class TezosTransactionTest {

    val blockchain = Blockchain.Tezos

    @Test
    fun buildContentsFromCorrectData() {
        // arrange
        val walletPublicKey = "64DF67680F2167E1A085083FE3085561E6BEF5AA1FC165785FFAE6264706DB8C"
            .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "KT1G1ARHx4Ujry8PzTis6HLNwQ29rYQY2Nay"
        val counter = 1561L
        val isPublicKeyRevealed = false

        val walletAddress = TezosAddressService().makeAddress(
            publicKey = walletPublicKey.wrapInObject(),
            addressType = AddressType.Default,
            curve = EllipticCurve.Ed25519
        ).value

        val transactionBuilder = TezosTransactionBuilder(walletPublicKey, EllipticCurve.Ed25519)
        transactionBuilder.counter = counter

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee
        )

        val expectedContent1 = TezosOperationContent(
            kind = "reveal",
            source = "tz1W3nEJjDmQrZRK72jAzZczWmErfGLUaMuN",
            fee = "1300",
            counter = "1562",
            gas_limit = "10000",
            storage_limit = "0",
            public_key = "edpkuQebQ8VAWLM7Hb1dFQJm8gmhfn5UabsNmtg7fJuepAPag4gpRF"
        )
        val expectedContent2 = TezosOperationContent(
            kind = "transaction",
            source = "tz1W3nEJjDmQrZRK72jAzZczWmErfGLUaMuN",
            fee = "1420",
            counter = "1563",
            gas_limit = "10600",
            storage_limit = "300",
            destination = "KT1G1ARHx4Ujry8PzTis6HLNwQ29rYQY2Nay",
            amount = "100000"
        )

        // act
        val buildContentsResult = transactionBuilder
            .buildContents(transactionData, isPublicKeyRevealed) as Result.Success

        // assert
        Truth.assertThat(buildContentsResult.data).containsExactly(expectedContent1, expectedContent2)
    }

    @Test
    fun buildToSign() {
        val forgedContents =
            "b336b959766f8b6a7771b59334025f31af28ec13dfedca8b2049539361b079ef6b00722f2b1cf1abe10aeef0875211d0406edcb9e292940a9a0c904e000064df67680f2167e1a085083fe3085561e6bef5aa1fc165785ffae6264706db8c6c00722f2b1cf1abe10aeef0875211d0406edcb9e2928c0b9b0ce852ac02a08d06015165f0e9a47be7958c6250a0b52fd53dc2321df30000"
        val expectedHashToSign = "545BBB1561486843BE9D923B6BA578646612082BCF53FAD8E1A5D30C3FC0A998"
            .hexToBytes()

        val hashToSign = TezosTransactionBuilder("".toByteArray(), EllipticCurve.Ed25519).buildToSign(forgedContents)

        Truth.assertThat(hashToSign).isEqualTo(expectedHashToSign)
    }

    @Test
    fun buildToSend() {
        val forgedContents =
            "b336b959766f8b6a7771b59334025f31af28ec13dfedca8b2049539361b079ef6b00722f2b1cf1abe10aeef0875211d0406edcb9e292940a9a0c904e000064df67680f2167e1a085083fe3085561e6bef5aa1fc165785ffae6264706db8c6c00722f2b1cf1abe10aeef0875211d0406edcb9e2928c0b9b0ce852ac02a08d06015165f0e9a47be7958c6250a0b52fd53dc2321df30000"
        val signature =
            "B945398FB90158761F6D61789B594D042F0F490F9656FBFFAE8F18B49D5F30054F43EE43CCAB2703F0E2E4E61D99CF3D4A875CD759569787CF0AED02415434C6"
                .hexToBytes()
        val expectedSignedTransaction =
            "b336b959766f8b6a7771b59334025f31af28ec13dfedca8b2049539361b079ef6b00722f2b1cf1abe10aeef0875211d0406edcb9e292940a9a0c904e000064df67680f2167e1a085083fe3085561e6bef5aa1fc165785ffae6264706db8c6c00722f2b1cf1abe10aeef0875211d0406edcb9e2928c0b9b0ce852ac02a08d06015165f0e9a47be7958c6250a0b52fd53dc2321df30000B945398FB90158761F6D61789B594D042F0F490F9656FBFFAE8F18B49D5F30054F43EE43CCAB2703F0E2E4E61D99CF3D4A875CD759569787CF0AED02415434C6"

        val signedTransaction = TezosTransactionBuilder("".toByteArray(), EllipticCurve.Ed25519)
            .buildToSend(signature, forgedContents)

        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}