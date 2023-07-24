package com.tangem.blockchain.blockchains.binance

import com.google.common.truth.Truth
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class BinanceTransactionTest {

    val blockchain = Blockchain.Binance

    @Test
    fun buildCorrectCoinTransaction() {
        // arrange
        val walletPublicKey =
            "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376F"
                .hexToBytes()
        val signature =
            "337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "bnb1cewmwr9vnnrmy5qgl9eugnv8pm422373j5g3x2"
        val accountNumber = 4847L
        val sequence = 14L

        val transactionBuilder = BinanceTransactionBuilder(walletPublicKey)
        transactionBuilder.accountNumber = accountNumber
        transactionBuilder.sequence = sequence

        val walletAddress =
            BinanceAddressService().makeAddress(
                publicKey = Wallet.PublicKey(walletPublicKey, null),
                addressType = AddressType.Default,
                curve = EllipticCurve.Secp256k1
            ).value
        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee
        )

        val expectedHashToSign = "796D08DBC0A4DA42C03F1DC3365FA2D5A8C2608F1949842B00A520A213238486"
            .hexToBytes()
        val expectedSignedTransaction =
            "C501F0625DEE0A4C2A2C87FA0A220A14D95DCA4F06B1F60BEE334ECDDA515E51DD6593CF120A0A03424E421080ADE20412220A14C65DB70CAC9CC7B25008F973C44D870EEAA547D1120A0A03424E421080ADE204126F0A26EB5AE9872103E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E1240337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC58318EF25200E2003"
                .hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData)
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat((buildToSignResult as Result.Success).data).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectTokenTransaction() {
        // arrange
        val walletPublicKey =
            "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376F"
                .hexToBytes()
        val signature =
            "337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "bnb1cewmwr9vnnrmy5qgl9eugnv8pm422373j5g3x2"
        val accountNumber = 4847L
        val sequence = 14L
        val token = Token(
            symbol = "BUSD",
            contractAddress = "BUSD-BD1",
            decimals = 8
        )

        val transactionBuilder = BinanceTransactionBuilder(walletPublicKey)
        transactionBuilder.accountNumber = accountNumber
        transactionBuilder.sequence = sequence

        val walletAddress = BinanceAddressService().makeAddress(
            publicKey = Wallet.PublicKey(walletPublicKey, null),
            addressType = AddressType.Default,
            curve = EllipticCurve.Secp256k1
        ).value
        val amountToSend = Amount(sendValue, blockchain, AmountType.Token(token))
        val fee = Fee.Common(Amount(feeValue, blockchain, AmountType.Coin))
        val transactionData = TransactionData(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee
        )

        val expectedHashToSign = "573AB75AE74CBB63E09B69F10C1953A0AEA2DA5C55F1C3435E1EB982CFA18D2F"
            .hexToBytes()
        val expectedSignedTransaction =
            "CF01F0625DEE0A562A2C87FA0A270A14D95DCA4F06B1F60BEE334ECDDA515E51DD6593CF120F0A08425553442D4244311080ADE20412270A14C65DB70CAC9CC7B25008F973C44D870EEAA547D1120F0A08425553442D4244311080ADE204126F0A26EB5AE9872103E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E1240337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC58318EF25200E2003"
                .hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData)
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat((buildToSignResult as Result.Success).data).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}