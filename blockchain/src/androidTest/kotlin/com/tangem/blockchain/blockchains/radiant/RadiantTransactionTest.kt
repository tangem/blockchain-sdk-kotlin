package com.tangem.blockchain.blockchains.radiant

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.network.electrum.ElectrumUnspentUTXORecord
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Test
import wallet.core.jni.PrivateKey

class RadiantTransactionTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val blockchain = Blockchain.Radiant
    private val privateKey = PrivateKey("079E750E71A7A2680380A4744C0E84567B1F8FC3C0AFD362D8326E1E676A4A15".hexToBytes())
    private val publicKey by lazy { privateKey.getPublicKeySecp256k1(true) }
    private val addressService = BitcoinAddressService(blockchain)

    @Test
    fun testSignRawTransaction() {
        val address = addressService.makeAddress(publicKey.data())
        Truth.assertThat("166w5AGDyvMkJqfDAtLbTJeoQh6FqYCfLQ").isEqualTo(address)

        val txBuilder = RadiantTransactionBuilder(
            publicKey = Wallet.PublicKey(seedKey = publicKey.data(), derivationType = null),
            decimals = blockchain.decimals(),
        )

        val utxo = listOf(
            ElectrumUnspentUTXORecord(
                txPos = 1,
                txHash = "4594c0fb5c5a8b4b2de2249fe704458e0e6e91e259258ca92d7c4e1067421e37",
                value = 68791375.toBigDecimal().movePointLeft(blockchain.decimals()),
                height = 203208,
                outpointHash = null,
            ),
            ElectrumUnspentUTXORecord(
                txPos = 0,
                txHash = "2ae842e688ce5e74390de3e9a4754d3f7438266ab0526ff880b9188bb4ea0c28",
                value = 100000000.toBigDecimal().movePointLeft(blockchain.decimals()),
                height = 203238,
                outpointHash = null,
            ),
            ElectrumUnspentUTXORecord(
                txPos = 0,
                txHash = "e2ea7fc29ca0dc4924f51f77918c8aff0c00fbb373a25509e928f78bf088cfcf",
                value = 10000000.toBigDecimal().movePointLeft(blockchain.decimals()),
                height = 203428,
                outpointHash = null,
            ),
            ElectrumUnspentUTXORecord(
                txPos = 0,
                txHash = "aba3aefb04a43dc026adc35972e4a20f8bdacd48074c143372f1b99cfb6bf8bc",
                value = 10000000.toBigDecimal().movePointLeft(blockchain.decimals()),
                height = 203439,
                outpointHash = null,
            ),
            ElectrumUnspentUTXORecord(
                txPos = 0,
                txHash = "ffaae960d8107cf4043dc32980c9634c9ff78fc69b2c6eb9b9c69a04a52ed179",
                value = 10000000.toBigDecimal().movePointLeft(blockchain.decimals()),
                height = 203500,
                outpointHash = null,
            ),
        )
        txBuilder.setUnspentOutputs(utxo)

        val amountValueDecimal = 1000.toBigDecimal().movePointLeft(blockchain.decimals())
        val amountValue = Amount(blockchain = blockchain, value = amountValueDecimal)
        val feeValue = Amount(blockchain = blockchain, value = "0.1".toBigDecimal())

        val transaction = TransactionData.Uncompiled(
            amount = amountValue,
            fee = Fee.Common(feeValue),
            sourceAddress = address,
            destinationAddress = "1vr9gJkNzTHv8DEQb4QBxAnQCxgzkFkbf",
        )

        val hashesForSign = txBuilder.buildForSign(transaction)
        val hexHashesForSign = hashesForSign.map { it.toHexString() }

        val expectedHashesForSign = listOf("A0A18B971380B7C0C37F0F2B05B11119456BE5BC4A2AC758E18A3571C4D488B8")
        Truth.assertThat(hexHashesForSign).isEqualTo(expectedHashesForSign)

        val expectedSignatures = listOf(
            "4F7895FE109BB36400D6B77C7E766C84EC9E1D184DD2663C150EB21420DF93DA08CD71BC7F53C68C847B5D074A1832DA52E29F87EBD2DF0DDE86089301B4836100",
        )
        Truth.assertThat(expectedSignatures.count()).isEqualTo(hashesForSign.count())

        val signatures = List(hashesForSign.size) { index ->
            expectedSignatures[index].hexToBytes()
        }
        val rawTransaction = txBuilder.buildForSend(transactionData = transaction, signatures = signatures)
        val expectedRawTransaction =
            "0100000001371e4267104e7c2da98c2559e2916e0e8e4504e79f24e22d4b8b5a5cfbc09445010000006a47304402204f7895fe109bb36400d6b77c7e766c84ec9e1d184dd2663c150eb21420df93da022008cd71bc7f53c68c847b5d074a1832da52e29f87ebd2df0dde86089301b48361412103d6fde463a4d0f4decc6ab11be24e83c55a15f68fd5db561eebca021976215ff5ffffffff02e8030000000000001976a9140a2f12f228cbc244c745f33a23f7e924cbf3b6ad88ace7118103000000001976a91437f7d8745fb391f80384c4c375e6e884ee4cec2888ac00000000"
        Truth.assertThat(rawTransaction.toHexString().lowercase()).isEqualTo(expectedRawTransaction)
    }
}