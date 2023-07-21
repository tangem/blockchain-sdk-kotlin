package com.tangem.blockchain.blockchains.kaspa

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.kaspa.network.KaspaInput
import com.tangem.blockchain.blockchains.kaspa.network.KaspaOutput
import com.tangem.blockchain.blockchains.kaspa.network.KaspaPreviousOutpoint
import com.tangem.blockchain.blockchains.kaspa.network.KaspaScriptPublicKey
import com.tangem.blockchain.blockchains.kaspa.network.KaspaTransactionBody
import com.tangem.blockchain.blockchains.kaspa.network.KaspaTransactionData
import com.tangem.blockchain.blockchains.kaspa.network.KaspaUnspentOutput
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class KaspaTransactionTest {

    private val blockchain = Blockchain.Kaspa
    private val decimals = blockchain.decimals()
    private val addressService = KaspaAddressService()

    @Test
    fun buildCorrectTransactionToSchnorrAddress() {
        // arrange
        val walletPublicKey = "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
                .hexToBytes()
        val signature =
            "E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC40168"
                .hexToBytes()
        val sendValue = "0.001".toBigDecimal()
        val feeValue = "0.0003".toBigDecimal()
        val destinationAddress = "kaspa:qpsqw2aamda868dlgqczeczd28d5nc3rlrj3t87vu9q58l2tugpjs2psdm4fv"

        val sourceAddress = addressService.makeAddressWithDefaultType(walletPublicKey)

        val transactionBuilder = KaspaTransactionBuilder()
        transactionBuilder.unspentOutputs = listOf(
            KaspaUnspentOutput(
                transactionHash = "deb88e7dd734437c6232a636085ef917d1d13cc549fe14749765508b2782f2fb".hexToBytes(),
                outputIndex = 0,
                amount = 10000000.toBigDecimal().movePointLeft(decimals),
                outputScript = "21034c88a1a83469ddf20d0c07e5c4a1e7b83734e721e60d642b94a53222c47c670dab".hexToBytes()
            ),
            KaspaUnspentOutput(
                transactionHash = "304db39069dc409acedf544443dcd4a4f02bfad4aeb67116f8bf087822c456af".hexToBytes(),
                outputIndex = 0,
                amount = 10000000.toBigDecimal().movePointLeft(decimals),
                outputScript = "21034c88a1a83469ddf20d0c07e5c4a1e7b83734e721e60d642b94a53222c47c670dab".hexToBytes()
            ),
            KaspaUnspentOutput(
                transactionHash = "ae96e819429e9da538e84cb213f62fbc8ad32e932d7c7f1fb9bd2fedf8fd7b4a".hexToBytes(),
                outputIndex = 0,
                amount = 500000000.toBigDecimal().movePointLeft(decimals),
                outputScript = "21034c88a1a83469ddf20d0c07e5c4a1e7b83734e721e60d642b94a53222c47c670dab".hexToBytes()
            )
        )

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = sourceAddress,
                destinationAddress = destinationAddress,
                amount = amountToSend,
                fee = fee
        )

        val expectedHashToSign1 = "F5080102132C6DAB382DE67A427F1DF560BA7F5F0D7FA4DFA535C474761423C2"
                .hexToBytes().toList()
        val expectedHashToSign2 = "90767E75D102556256E4B3C76F341292FDDBEF1683C49E3C03AC16A83FD1FB83"
                .hexToBytes().toList()
        val expectedHashToSign3 = "F9738FE93426667581DB4BA1AE4F432F384C393D0F098D3A9AA6087C4F62C4A4"
            .hexToBytes().toList()
        val expectedSignedTransaction = KaspaTransactionBody(
            KaspaTransactionData(
                version = 0,
                inputs = listOf(
                    KaspaInput(
                        previousOutpoint = KaspaPreviousOutpoint(
                            transactionId = "ae96e819429e9da538e84cb213f62fbc8ad32e932d7c7f1fb9bd2fedf8fd7b4a",
                            index = 0
                        ),
                        signatureScript = "41E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC4016501",
                        sequence = 0,
                        sigOpCount = 1
                    ),
                    KaspaInput(
                        previousOutpoint = KaspaPreviousOutpoint(
                            transactionId = "deb88e7dd734437c6232a636085ef917d1d13cc549fe14749765508b2782f2fb",
                            index = 0
                        ),
                        signatureScript = "414BF71C43DF96FC6B46766CAE30E97BD9018E9B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC58301",
                        sequence = 0,
                        sigOpCount = 1
                    ),
                    KaspaInput(
                        previousOutpoint = KaspaPreviousOutpoint(
                            transactionId = "304db39069dc409acedf544443dcd4a4f02bfad4aeb67116f8bf087822c456af",
                            index = 0
                        ),
                        signatureScript = "41E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B07C30984424B13344F0A7CC4016801",
                        sequence = 0,
                        sigOpCount = 1
                    )
                ),
                outputs = listOf(
                    KaspaOutput(
                        amount = 100000,
                        scriptPublicKey = KaspaScriptPublicKey(
                            scriptPublicKey = "2060072BBDDB7A7D1DBF40302CE04D51DB49E223F8E5159FCCE14143FD4BE20328AC",
                            version = 0
                        )
                    ),
                    KaspaOutput(
                        amount = 519870000,
                        scriptPublicKey = KaspaScriptPublicKey(
                            scriptPublicKey = "2103EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FAB",
                            version = 0
                        )
                    )
                ),
                lockTime = 0,
                subnetworkId = "0000000000000000000000000000000000000000"
            )
        )

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() })
                .containsExactly(expectedHashToSign1, expectedHashToSign2, expectedHashToSign3)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectTransactionToP2SHAddress() {
        // arrange
        val walletPublicKey = "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF8882A3125B0EE9AE96DDDE1AE743F"
            .hexToBytes()
        val signature = "E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B0704EB30400CE9D1DEED12B84D41"
            .hexToBytes()
        val sendValue = "0.001".toBigDecimal()
        val feeValue = "0.0001".toBigDecimal()
        val destinationAddress = "kaspa:pqurku73qluhxrmvyj799yeyptpmsflpnc8pha80z6zjh6efwg3v2rrepjm5r"

        val sourceAddress = addressService.makeAddressWithDefaultType(walletPublicKey)

        val transactionBuilder = KaspaTransactionBuilder()
        transactionBuilder.unspentOutputs = listOf(
            KaspaUnspentOutput(
                transactionHash = "ae96e819429e9da538e84cb213f62fbc8ad32e932d7c7f1fb9bd2fedf8fd7b4a".hexToBytes(),
                outputIndex = 0,
                amount = 500000000.toBigDecimal().movePointLeft(decimals),
                outputScript = "21034c88a1a83469ddf20d0c07e5c4a1e7b83734e721e60d642b94a53222c47c670dab".hexToBytes()
            )
        )

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee
        )

        val expectedHashToSign1 = "C550515D34A091D7F3D2827286E7AEF685ECE9C0BBCCB4B08BC65F6EBD83E8F2"
            .hexToBytes().toList()
        val expectedSignedTransaction = KaspaTransactionBody(
            KaspaTransactionData(
                version = 0,
                inputs = listOf(
                    KaspaInput(
                        previousOutpoint = KaspaPreviousOutpoint(
                            transactionId = "ae96e819429e9da538e84cb213f62fbc8ad32e932d7c7f1fb9bd2fedf8fd7b4a",
                            index = 0
                        ),
                        signatureScript = "41E2747D4E00C55D69FA0B8ADFAFD07F41144F888E322D377878E83F25FD2E258B2E918EF79E151337D7F3BD0798D66FDCE04B0704EB30400CE9D1DEED12B84D4101",
                        sequence = 0,
                        sigOpCount = 1
                    )
                ),
                outputs = listOf(
                    KaspaOutput(
                        amount = 100000,
                        scriptPublicKey = KaspaScriptPublicKey(
                            scriptPublicKey = "AA20383B73D107F9730F6C24BC5293240AC3B827E19E0E1BF4EF16852BEB297222C587",
                            version = 0
                        )
                    ),
                    KaspaOutput(
                        amount = 499890000,
                        scriptPublicKey = KaspaScriptPublicKey(
                            scriptPublicKey = "2103EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FAB",
                            version = 0
                        )
                    )
                ),
                lockTime = 0,
                subnetworkId = "0000000000000000000000000000000000000000"
            )
        )

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() }).containsExactly(expectedHashToSign1)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }
}