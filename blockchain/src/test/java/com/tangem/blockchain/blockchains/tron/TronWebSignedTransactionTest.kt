package com.tangem.blockchain.blockchains.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.tron.network.BlockHeader
import com.tangem.blockchain.blockchains.tron.network.RawData
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronTransferTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import okio.ByteString.Companion.decodeHex
import org.json.JSONObject
import org.junit.Test
import java.math.BigDecimal

class TronWebSignedTransactionTest {

    private val blockchain = Blockchain.Tron
    private val builder = TronTransactionBuilder()

    private val tronBlock = TronBlock(
        blockHeader = BlockHeader(
            RawData(
                number = 3111739,
                txTrieRoot = "64288c2db0641316762a99dbb02ef7c90f968b60f9f2e410835980614332f86d",
                witnessAddress = "415863f6091b8e71766da808b1dd3159790f61de7d",
                parentHash = "00000000002f7b3af4f5f8b9e23a30c530f719f165b742e7358536b280eead2d",
                version = 3,
                timestamp = 1539295479000,
            ),
        ),
    )

    private val usdt = Token(
        symbol = "USDT",
        contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
        decimals = 6,
    )

    @Test
    fun `GIVEN signed token raw WHEN buildSignedTronWebJson THEN matches TronWeb shape`() {
        // Arrange
        val destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn"
        val source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF"
        val amount = Amount(token = usdt, value = BigDecimal("1.5"))
        val extras = TronTransactionExtras(
            callData = TronTransferTokenCallData(destination = destination, amount = amount),
        )
        val raw = builder.buildForSign(
            amount = amount,
            source = source,
            destination = destination,
            block = tronBlock,
            extras = extras,
        )
        val signature = (
            "6b5de85a80b2f4f02351f691593fb0e49f14c5cb42451373485357e42d7890cd" +
                "77ad7bfcb733555c098b992da79dabe5050f5e2db77d9d98f199074222de037701"
            ).decodeHex().toByteArray()

        // Act
        val json = builder.buildSignedTronWebJson(raw, signature)

        // Assert
        val obj = JSONObject(json)
        val rawBytes = raw.encode()
        assertThat(obj.getBoolean("visible")).isFalse()
        assertThat(obj.getString("txID")).isEqualTo(rawBytes.calculateSha256().toHexString().lowercase())
        assertThat(obj.getString("raw_data_hex")).isEqualTo(rawBytes.toHexString().lowercase())
        assertThat(obj.getJSONArray("signature").getString(0))
            .isEqualTo(signature.toHexString().lowercase())

        val contract = obj.getJSONObject("raw_data").getJSONArray("contract").getJSONObject(0)
        assertThat(contract.getString("type")).isEqualTo("TriggerSmartContract")
        val parameter = contract.getJSONObject("parameter")
        assertThat(parameter.getString("type_url"))
            .isEqualTo("type.googleapis.com/protocol.TriggerSmartContract")
        val value = parameter.getJSONObject("value")
        assertThat(value.getString("data"))
            .isEqualTo(
                "a9059cbb" +
                    "000000000000000000000041ec8c5a0fcbb28f14418eed9cf582af0d77e4256e" +
                    "000000000000000000000000000000000000000000000000000000000016e360",
            )
        assertThat(value.getString("owner_address"))
            .isEqualTo("41c5d1c75825b30bb2e2e655798209d56448eb6b5e")
        assertThat(value.getString("contract_address"))
            .isEqualTo("41a614f803b6fd780986a42c78ec9c7f77e6ded13c")
    }

    @Test
    fun `GIVEN coin transfer raw WHEN buildSignedTronWebJson THEN throws IllegalArgumentException`() {
        // Arrange
        val source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF"
        val destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn"
        val coinAmount = Amount(java.math.BigDecimal.valueOf(1), blockchain)
        val coinRaw = builder.buildForSign(
            amount = coinAmount,
            source = source,
            destination = destination,
            block = tronBlock,
            extras = null,
        )

        // Act
        val result = runCatching { builder.buildSignedTronWebJson(coinRaw, ByteArray(0)) }

        // Assert
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }
}