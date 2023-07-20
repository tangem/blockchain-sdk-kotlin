package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.wrapInObject
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptBuilder
import org.junit.Test

class BitcoinTransactionTest {

    private val blockchain = Blockchain.Bitcoin
    private val networkParameters = MainNetParams()

    @Test
    fun buildCorrectTransaction() {
        // arrange
        val walletPublicKey = "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376F"
                .hexToBytes()
        val signature = "88E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE30E97BD9018E9B2E918EF79E15E2747D4E00C55D69FA0B8ADFAFD07F41144F81337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583"
                .hexToBytes()
        val sendValue = "0.1".toBigDecimal()
        val feeValue = "0.01".toBigDecimal()
        val destinationAddress = "34gJYef7yHBmRhnmKzrXKJddWMzCuFkbBY"


        val legacyAddress = BitcoinAddressService(blockchain).makeAddress(
            walletPublicKey.wrapInObject(), AddressType.Legacy
        )

        val segwitAddress = BitcoinAddressService(blockchain).makeAddress(
            walletPublicKey.wrapInObject(), AddressType.Default
        )

        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, listOf(legacyAddress, segwitAddress))
        transactionBuilder.unspentOutputs =
                prepareTwoUnspentOutputs(listOf(legacyAddress.value, segwitAddress.value), networkParameters)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = segwitAddress.value,
                destinationAddress = destinationAddress,
                amount = amountToSend,
                fee = fee
        )

        val expectedHashToSign1 = "000D25B43DE238A2FB30C00F138413B9E4E32F45B5F831EF4312D9A2CEBDD62F"
                .hexToBytes().toList()
        val expectedHashToSign2 = "26C937660455A32DB3CF79D16BBD1D835BA34A84A19381DEE4FAD05884DD4453"
                .hexToBytes().toList()
        val expectedSignedTransaction = "01000000000102B6A2673BDD04D57B5560F4E46CAC3C1F974E41463568F2A11E7D3175521D9C6D000000008B48304502210088E322D377878E83F25FD2E258344F0A7CC401654BF71C43DF96FC6B46766CAE022030E97BD9018E9B2E918EF79E15E2747D4E00C55D69FA0B8ADFAFD07F41144F81014104E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376FFFFFFFFF3F86D67DC12F3E3E7EE47E3B02D30D476823B594CBCABF1123A8C272CC91F2AE4900000000FFFFFFFF02809698000000000017A91420C5D650F5A66352A0D07C526D5738586F09E2DF8780790CD4E8000000160014D95DCA4F06B1F60BEE334ECDDA515E51DD6593CF00024730440220337D7F3BD0798D66FDCE04B07C30984424B13B98BB2C3645744A696AD26ECC7802200157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D33A877FC583012103E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E00000000"
                .hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() })
                .containsExactly(expectedHashToSign1, expectedHashToSign2)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    @Test
    fun buildCorrectMultisigTransaction() {
        // arrange
        val walletPublicKey = "04D2B9FB288540D54E5B32ECAF0381CD571F97F6F1ECD036B66BB11AA52FFE9981110D883080E2E255C6B1640586F7765E6FAA325D1340F49B56B83D9DE56BC7ED"
                .hexToBytes()
        val pairPublicKey = "0485D520C8B907F0BC5E03FCBBAC212CCD270764BBFF4990A28653A2FB0D656C342DF143C4D52C43582289E20A81D5D014C1384A1FFFEA1D121903AD7ED35A01EA"
                .hexToBytes()
        val signature = "B8ADFAFD07F41144F81ECC780157EA9D44DC41D0BCB420175A5D3F543079F4263AA2DBDE0EE2D388E322D37784424B13B98BB2C3645744A696AD2696FC6B46766CAE30E97BD9018E9B2E918EF79E878E83F25FD2E258344F0A7CC401654BF71C43DF337D7F3BD0798D66FDCE04B07C30915E2747D4E00C55D69FA03A877FC583"
                .hexToBytes()
        val sendValue = "1.7".toBigDecimal()
        val feeValue = "0.3".toBigDecimal()
        val destinationAddress = "1CM45rkJXtV9r8aUXeJnVKUh174EcKBQAJ"

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey.wrapInObject(), pairPublicKey)
        val legacyAddress = addresses.find { it.type == AddressType.Legacy }!!.value
        val segwitAddress = addresses.find { it.type == AddressType.Default }!!.value
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs =
                prepareTwoUnspentOutputs(listOf(legacyAddress, segwitAddress), networkParameters)

        val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
        val fee = Fee.Common(Amount(amountToSend, feeValue))
        val transactionData = TransactionData(
                sourceAddress = segwitAddress,
                destinationAddress = destinationAddress,
                amount = amountToSend,
                fee = fee
        )

        val expectedHashToSign1 = "21978B183F5371AA1D4CCF041E3EAE941E591C41D7258E6E3AA28788A712ECD6"
                .hexToBytes().toList()
        val expectedHashToSign2 = "F8B6EA97BE48DD5A1F7376546BFA916ABCFBE77593FE1FEC02C14FD2F75E0E27"
                .hexToBytes().toList()
        val expectedSignedTransaction = "01000000000102B6A2673BDD04D57B5560F4E46CAC3C1F974E41463568F2A11E7D3175521D9C6D000000009200483045022100B8ADFAFD07F41144F81ECC780157EA9D44DC41D0BCB420175A5D3F543079F42602203AA2DBDE0EE2D388E322D37784424B13B98BB2C3645744A696AD2696FC6B4676014751210285D520C8B907F0BC5E03FCBBAC212CCD270764BBFF4990A28653A2FB0D656C342103D2B9FB288540D54E5B32ECAF0381CD571F97F6F1ECD036B66BB11AA52FFE998152AEFFFFFFFF3F86D67DC12F3E3E7EE47E3B02D30D476823B594CBCABF1123A8C272CC91F2AE4900000000FFFFFFFF0280FE210A000000001976A9147C7446EC6D27F4093D51CAFE09BC5F29BF6C806388AC4090C8C8E80000002200204867181DE1B7DB7F100CEF2D09A97747C87FA92862FB5DBCA09B0394CDC0190B00030047304402206CAE30E97BD9018E9B2E918EF79E878E83F25FD2E258344F0A7CC401654BF71C022043DF337D7F3BD0798D66FDCE04B07C30915E2747D4E00C55D69FA03A877FC583014751210285D520C8B907F0BC5E03FCBBAC212CCD270764BBFF4990A28653A2FB0D656C342103D2B9FB288540D54E5B32ECAF0381CD571F97F6F1ECD036B66BB11AA52FFE998152AE00000000"
                .hexToBytes()

        // act
        val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
        val signedTransaction = transactionBuilder.buildToSend(signature)

        // assert
        Truth.assertThat(buildToSignResult.data.map { it.toList() })
                .containsExactly(expectedHashToSign1, expectedHashToSign2)
        Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
    }

    companion object {
        fun prepareTwoUnspentOutputs(
                addresses: List<String>,
                networkParameters: NetworkParameters
        ): List<BitcoinUnspentOutput> {

            if (addresses.isEmpty()) throw Exception("Address is needed to prepare utxo")
            val bitcoinAddresses = addresses.map { Address.fromString(networkParameters, it) }
            val utxo1 = BitcoinUnspentOutput(
                    amount = "10000".toBigDecimal(),
                    outputIndex = 0,
                    transactionHash = "6d9c1d5275317d1ea1f2683546414e971f3cac6ce4f460557bd504dd3b67a2b6"
                            .hexToBytes(),
                    outputScript = ScriptBuilder.createOutputScript(bitcoinAddresses[0]).program
            )
            val utxo2 = BitcoinUnspentOutput(
                    amount = "0.01".toBigDecimal(),
                    outputIndex = 73,
                    transactionHash = "aef291cc72c2a82311bfcacb94b52368470dd3023b7ee47e3e3e2fc17dd6863f"
                            .hexToBytes(),
                    outputScript = ScriptBuilder.createOutputScript(
                            bitcoinAddresses.getOrNull(1) ?: bitcoinAddresses[0]
                    ).program
            )
            return listOf(utxo1, utxo2)
        }
    }
}