package com.tangem.blockchain.blockchains.ethereum

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Before
import org.junit.Test

class EthereumTransactionBuilderTest {

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
    fun buildCorrectCoinTransaction() {
        // arrange
        val blockchain = Blockchain.Ethereum
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
        val signature =
            "B945398FB90158761F6D61789B594D042F0F490F9656FBFFAE8F18B49D5F30054F43EE43CCAB2703F0E2E4E61D99CF3D4A875CD759569787CF0AED02415434C6"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()
        val walletAddress = "0xb1123efF798183B7Cb32F62607D3D39E950d9cc3"

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Ethereum.Legacy(
            amount = Amount(amountToSend, feeValue),
            gasLimit = "21000".toBigInteger(),
            gasPrice = "476190476190".toBigInteger(),
        )
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(nonce = nonce),
        )

        val expectedHashToSign = "BDBECF64B443F82D1F9FDA3F2D6BA69AF6D82029B8271339B7E775613AE57761".hexToBytes()
        val expectedSignedTransaction =
            "F86C0F856EDF2A079E825208947655B9B19FFAB8B897F836857DAE22A1E7F8D73588016345785D8A00008025A0B945398FB90158761F6D61789B594D042F0F490F9656FBFFAE8F18B49D5F3005A04F43EE43CCAB2703F0E2E4E61D99CF3D4A875CD759569787CF0AED02415434C6"
                .hexToBytes()

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
        val blockchain = Blockchain.Ethereum
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
        val signature =
            "F408C40F8D8B4A40E35502355C87FBBF218EC9ECB036D42DAA6211EAD4498A6FBC800E82CB2CC0FAB1D68FD3F8E895EC3E0DCB5A05342F5153210142E4224D4C"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "0x7655b9b19ffab8b897f836857dae22a1e7f8d735"
        val nonce = 15.toBigInteger()
        val contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        val token = Token(
            name = "USDC Coin",
            symbol = "USDC",
            contractAddress = contractAddress,
            decimals = 6,
        )

        val walletAddress = "0xb1123efF798183B7Cb32F62607D3D39E950d9cc3"

        val amountToSend = Amount(sendValue, blockchain, AmountType.Token(token))
        val fee = Fee.Ethereum.Legacy(
            amount = Amount(feeValue, blockchain, AmountType.Coin),
            gasLimit = "21000".toBigInteger(),
            gasPrice = "476190476190".toBigInteger(),
        )
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(
                callData = TransferERC20TokenCallData(
                    destination = destinationAddress,
                    amount = amountToSend,
                ),
                nonce = nonce,
            ),
        )

        val expectedHashToSign = "2F47B058A0C4A91EC6E26372FA926ACB899235D7A639565B4FC82C7A9356D6C5".hexToBytes()
        val expectedSignedTransaction =
            "F8A90F856EDF2A079E82520894A0B86991C6218B36C1D19D4A2E9EB0CE3606EB4880B844A9059CBB0000000000000000000000007655B9B19FFAB8B897F836857DAE22A1E7F8D735000000000000000000000000000000000000000000000000016345785D8A000025A0F408C40F8D8B4A40E35502355C87FBBF218EC9ECB036D42DAA6211EAD4498A6FA0437FF17D34D33F054E29702C07176A127CA1118CAA1470EA6CB15D49EC13F3F5"
                .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectEip1559CoinTransaction() {
        // arrange
        val blockchain = Blockchain.Polygon
        val walletPublicKey =
            "043b08e56e38404199eb3320f32fdc7557029d4a4c39adae01cc47afd86cfa9a25fcbfaa2acda3ab33560a1d482a2088f3bb2c7b313fd11f50dd8fe508165d4ecf"
                .hexToBytes()
        val signature =
            "56DF71FF2A7FE93D2363056FE5FF32C51E5AC71733AF23A82F3974CB872537E95B60D6A0042CC34724DB84E949EEC8643761FE9027E9E7B1ED3DA23D8AB7C0A4"
                .hexToBytes()

        val sendValue = "1".toBigDecimal()
        val destinationAddress = "0x90e4d59c8583e37426b37d1d7394b6008a987c67"
        val nonce = 196.toBigInteger()
        val walletAddress = "0x29010F8F91B980858EB298A0843264cfF21Fd9c9"

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Ethereum.EIP1559(
            amount = Amount("0.01".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "21000".toBigInteger(),
            maxFeePerGas = "4478253867089".toBigInteger(),
            priorityFee = "31900000000".toBigInteger(),
        )

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = destinationAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(nonce = nonce),
        )

        val transactionBuilder = EthereumTransactionBuilder.create(
            wallet = Wallet(
                blockchain = blockchain,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
                tokens = setOf(),
            ),
        )

        val expectedHashToSign = "925f1debbb96941544aefe6a5532508e51f2b8ae1f3a911abfb24b83af610400".hexToBytes()
        val expectedSignedTransaction =
            "0x02f877818981c485076d635f00860412acbb20518252089490e4d59c8583e37426b37d1d7394b6008a987c67880de0b6b3a764000080c080a056df71ff2a7fe93d2363056fe5ff32c51e5ac71733af23a82f3974cb872537e9a05b60d6a0042cc34724db84e949eec8643761fe9027e9e7b1ed3da23d8ab7c0a4"
                .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectEip1559TokenTransaction() {
        // arrange
        val blockchain = Blockchain.Polygon
        val walletPublicKey =
            "043b08e56e38404199eb3320f32fdc7557029d4a4c39adae01cc47afd86cfa9a25fcbfaa2acda3ab33560a1d482a2088f3bb2c7b313fd11f50dd8fe508165d4ecf"
                .hexToBytes()

        val signature =
            "b8291b199416b39434f3c3b8cfd273afb41fa25f2ae66f8a4c56b08ad1749a122148b8bbbdeb7761031799ffbcbc7c0ee1dd4482f516bd6a33387ea5bce8cb7d"
                .hexToBytes()

        val walletAddress = "0x29010F8F91B980858EB298A0843264cfF21Fd9c9"
        val destinationAddress = "0x90e4d59c8583e37426b37d1d7394b6008a987c67"
        val contractAddress = "0xc2132d05d31c914a87c6611c10748aeb04b58e8f"

        val token = Token(
            name = "Tether",
            symbol = "USDT",
            contractAddress = contractAddress,
            decimals = 6,
        )

        val nonce = 195.toBigInteger()
        val sendValue = "1".toBigDecimal()

        val amountToSend = Amount(
            value = sendValue,
            currencySymbol = token.symbol,
            decimals = 6,
            type = AmountType.Token(token),
        )
        val fee = Fee.Ethereum.EIP1559(
            amount = Amount("0.1".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "47525".toBigInteger(),
            maxFeePerGas = "138077377799".toBigInteger(),
            priorityFee = "30000000000".toBigInteger(),
        )

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = contractAddress,
            amount = amountToSend,
            fee = fee,
            extras = EthereumTransactionExtras(
                nonce = nonce,
                callData = TransferERC20TokenCallData(
                    destination = destinationAddress,
                    amount = amountToSend,
                ),
            ),
        )

        val transactionBuilder = EthereumTransactionBuilder.create(
            wallet = Wallet(
                blockchain = blockchain,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
                tokens = setOf(token),
            ),
        )

        val expectedHashToSign =
            "7843727fd03b42156222548815759dda5ac888033372157edffdde58fc05eff5".hexToBytes()
        val expectedSignedTransaction =
            "0x02f8b3818981c38506fc23ac008520260d950782b9a594c2132d05d31c914a87c6611c10748aeb04b58e8f80b844a9059cbb00000000000000000000000090e4d59c8583e37426b37d1d7394b6008a987c6700000000000000000000000000000000000000000000000000000000000f4240c080a0b8291b199416b39434f3c3b8cfd273afb41fa25f2ae66f8a4c56b08ad1749a12a02148b8bbbdeb7761031799ffbcbc7c0ee1dd4482f516bd6a33387ea5bce8cb7d"
                .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectEip1559TokenApprove() {
        // arrange
        val blockchain = Blockchain.Base
        val walletPublicKey =
            "04c0b0bebaf7cec052a1fb2919c83a3d192713a65c3675a22ad9a2f76d5da1cfb0d4fec9da0bc71b5a405758a2e0349e2d151bfff6ec3d50441f0adb947a8a44a1"
                .hexToBytes()
        val signature =
            "cc6163663ccdadf4489e9753b0307c0fb1eed7fe92a7b0a6b3cb0f6d24f9109e7dd41e4e30c6777b27688527af3c4ec69ed053246ca05d1b3b8c3da127c30eb0"
                .hexToBytes()

        val walletAddress = "0xF686Cc42C39e942D5B4a237286C5A55B451bD6F0"
        val spender = "0x111111125421cA6dc452d289314280a0f8842A65"
        val contractAddress = "0x940181a94a35a4569e4529a3cdfb74e38fd98631"
        val nonce = 10.toBigInteger()

        val fee = Fee.Ethereum.EIP1559(
            amount = Amount("0".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "47000".toBigInteger(),
            maxFeePerGas = "7250107".toBigInteger(),
            priorityFee = "2000000".toBigInteger(),
        )

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = contractAddress,
            amount = Amount("0".toBigDecimal(), blockchain, AmountType.Coin),
            fee = fee,
            extras = EthereumTransactionExtras(
                callData = ApprovalERC20TokenCallData(
                    spenderAddress = spender,
                    amount = null,
                ),
                nonce = nonce,
            ),
        )

        val transactionBuilder = EthereumTransactionBuilder.create(
            wallet = Wallet(
                blockchain = blockchain,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
                tokens = setOf(),
            ),
        )

        val expectedHashToSign = "bbada4215ac1d69b8c30449afd4dae224d32177c5a80877d4220756ad14b9852".hexToBytes()
        val expectedSignedTx =
            "02f8af8221050a831e8480836ea0bb82b79894940181a94a35a4569e4529a3cdfb74e38fd9863180b844095ea7b3000000000000000000000000111111125421ca6dc452d289314280a0f8842a65ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc001a0cc6163663ccdadf4489e9753b0307c0fb1eed7fe92a7b0a6b3cb0f6d24f9109ea07dd41e4e30c6777b27688527af3c4ec69ed053246ca05d1b3b8c3da127c30eb0"
                .hexToBytes()

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTx)
    }

    @Test
    fun buildCorrectEip1559TokenSwap() {
        // arrange
        val blockchain = Blockchain.Base
        val walletPublicKey =
            "04c0b0bebaf7cec052a1fb2919c83a3d192713a65c3675a22ad9a2f76d5da1cfb0d4fec9da0bc71b5a405758a2e0349e2d151bfff6ec3d50441f0adb947a8a44a1"
                .removePrefix("0x")
                .hexToBytes()

        val signature =
            "0982b50e820042d00a51ac23029cd66bdd88c6300890120be54a05afedbe938943e0e8f475dba0d9cd9d4a38f02e29662ef106c3bede1938230a32a2f23e8106"
                .removePrefix("0x")
                .hexToBytes()

        val walletAddress = "0xF686Cc42C39e942D5B4a237286C5A55B451bD6F0"
        val contractAddress = "0x111111125421ca6dc452d289314280a0f8842a65"

        val payload =
            "07ed2379000000000000000000000000e37e799d5077682fa0a244d46e5649f71457bd09000000000000000000000000940181a94a35a4569e4529a3cdfb74e38fd98631000000000000000000000000eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee000000000000000000000000e37e799d5077682fa0a244d46e5649f71457bd09000000000000000000000000f686cc42c39e942d5b4a237286c5a55b451bd6f0000000000000000000000000000000000000000000000000002386f26fc10000000000000000000000000000000000000000000000000000000002713e3fabbc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000012000000000000000000000000000000000000000000000000000000000000001410000000000000000000000000001230001090000f30000b700006800004e802026678dcd940181a94a35a4569e4529a3cdfb74e38fd98631beec796a4a2a27b687e1d48efad3805d7880052200000000000000000000000000000000000000000000000000002d79883d20000020d6bdbf78940181a94a35a4569e4529a3cdfb74e38fd9863102a0000000000000000000000000000000000000000000000000000002713e3fabbcee63c1e5003d5d143381916280ff91407febeb52f2b60f33cf940181a94a35a4569e4529a3cdfb74e38fd986314101420000000000000000000000000000000000000600042e1a7d4d0000000000000000000000000000000000000000000000000000000000000000c061111111125421ca6dc452d289314280a0f8842a6500206b4be0b9111111125421ca6dc452d289314280a0f8842a65000000000000000000000000000000000000000000000000000000000000002df1ec3e"
                .hexToBytes()

        val nonce = 11.toBigInteger()
        val fee = Fee.Ethereum.EIP1559(
            amount = Amount("0".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "156360".toBigInteger(),
            maxFeePerGas = "5672046".toBigInteger(),
            priorityFee = "1000000".toBigInteger(),
        )

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = walletAddress,
            destinationAddress = contractAddress,
            amount = Amount("0".toBigDecimal(), blockchain, AmountType.Coin),
            fee = fee,
            extras = EthereumTransactionExtras(
                callData = CompiledSmartContractCallData(payload),
                nonce = nonce,
            ),
        )

        val transactionBuilder = EthereumTransactionBuilder.create(
            wallet = Wallet(
                blockchain = blockchain,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null),
                tokens = setOf(),
            ),
        )

        val expectedHashToSign = "b59deacf74401648c860a4cfc9bee40d0da1502c05efa278d4afdb6dfe4bd8f3".hexToBytes()
        val expectedSignedTx = "02f903158221050b830f424083568c6e830262c894111111125421ca6dc452d289314280a0f8" +
            "842a6580b902a8${payload.toHexString()}c080a00982b50e820042d00a51ac23029cd66bdd88c6300890120be54a0" +
            "5afedbe9389a043e0e8f475dba0d9cd9d4a38f02e29662ef106c3bede1938230a32a2f23e8106"

        // act
        val transactionToSign = transactionBuilder.buildForSign(transactionData)
        val signedTransaction = transactionBuilder.buildForSend(transactionData, signature, transactionToSign)

        // assert
        Truth.assertThat(transactionToSign.hash).isEqualTo(expectedHashToSign)
        Truth.assertThat(signedTransaction.toHexString().lowercase())
            .isEqualTo(expectedSignedTx.lowercase())
    }

    @Test
    fun buildCorrectDummyTransactionForL1() {
        // arrange
        val blockchain = Blockchain.Ethereum
        val wallet = Wallet(
            blockchain = blockchain,
            addresses = setOf(),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65), derivationType = null),
            tokens = setOf(),
        )

        val transactionBuilder = EthereumTransactionBuilder.create(wallet)
        val destinationAddress = "0x90e4d59c8583e37426b37d1d7394b6008a987c67"
        val value = Amount("1".toBigDecimal(), blockchain, AmountType.Coin)

        val fee = Fee.Ethereum.EIP1559(
            amount = Amount("0".toBigDecimal(), blockchain, AmountType.Coin),
            gasLimit = "21000".toBigInteger(),
            maxFeePerGas = "4478253867089".toBigInteger(),
            priorityFee = "31900000000".toBigInteger(),
        )

        val expectedDummyTx = "EF928FCAC97DFD720631D2624C951CCD86191A260D05025879D8209A301D86FE".hexToBytes()

        // act
        val dummyTx = transactionBuilder.buildDummyTransactionForL1(
            amount = value,
            destination = destinationAddress,
            data = null,
            fee = fee,
        )

        // assert
        Truth.assertThat(dummyTx).isEqualTo(expectedDummyTx)
    }

    @Test
    fun parseBalance() {
        val parsed1 = EthereumUtils.parseEthereumDecimal("0x373c91e25f1040", decimalsCount = 18)
        val parsed2 = EthereumUtils.parseEthereumDecimal(
            "0x00000000000000000000000000000000000000000000000000373c91e25f1040",
            decimalsCount = 18,
        )

        Truth.assertThat(parsed1?.toPlainString()).isEqualTo("0.015547720984891456")
        Truth.assertThat(parsed2?.toPlainString()).isEqualTo("0.015547720984891456")

        val vbusdWithExtraZeros =
            "0x0000000000000000000000000000000000000000000000000000005a8c504ec900000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        val vbusdWithoutExtraZeros = "0x0000000000000000000000000000000000000000000000000000005a8c504ec9"
        val tooBig =
            "0x01234567890abcdef01234567890abcdef01234501234567890abcdef01234567890abcdef01234501234567890abcdef012345def01234501234567890abcdef012345def01234501234567890abcdef012345def01234501234567890abcdef01234567890abcdef012345"

        Truth.assertThat(EthereumUtils.parseEthereumDecimal(vbusdWithExtraZeros, 18)?.toPlainString())
            .isEqualTo("0.000000388901129929")

        Truth.assertThat(EthereumUtils.parseEthereumDecimal(vbusdWithoutExtraZeros, 18)?.toPlainString())
            .isEqualTo("0.000000388901129929")

        Truth.assertThat(EthereumUtils.parseEthereumDecimal(tooBig, 18)).isNull()
    }
}