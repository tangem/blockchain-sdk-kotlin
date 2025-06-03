package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString

class TronApprovalSmartContractTest {

    private val signature = "0x095ea7b3".hexToBytes()
    private val spenderAddress = "TG5wUqBkukAho2E38ca3EZG4zvYp3hUivZ"
    private val spenderData = "0000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3".hexToBytes()

    @Test
    fun makeLimitedAmountApprove() {
        val amount = "000000000000000000000000000000000000000000000000000000003b9aca00".hexToBytes()
        val expected = signature + spenderData + amount

        val actual = TronApprovalTokenCallData(
            spenderAddress = spenderAddress,
            amount = Amount(Blockchain.Tron).copy(
                value = "1000".toBigDecimal(),
            ),
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun makeUnlimitedAmountApprove() {
        val amount = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".hexToBytes()
        val expected = signature + spenderData + amount

        val actual = TronApprovalTokenCallData(
            spenderAddress = spenderAddress,
            amount = null,
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun makeWrongUnlimitedAmountApprove() {
        val amount = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".hexToBytes()
        val expected = signature + spenderData + amount

        val actual = TronApprovalTokenCallData(
            spenderAddress = spenderAddress,
            amount = Amount(blockchain = Blockchain.Tron),
        )

        Truth.assertThat(actual.dataHex).isNotEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isNotEqualTo(expected)
    }
}