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

    @Test
    fun decodeLimitedAmountApprove() {
        val callData = "0x095ea7b30000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        val actual = TronApprovalTokenCallData(callData)

        Truth.assertThat(actual?.spenderAddress).isEqualTo(spenderAddress)
        Truth.assertThat(actual?.amount?.value!!.compareTo("1000000000".toBigDecimal())).isEqualTo(0)
    }

    @Test
    fun decodeUnlimitedAmountApprove() {
        val callData = "0x095ea7b30000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        val actual = TronApprovalTokenCallData(callData)

        Truth.assertThat(actual?.spenderAddress).isEqualTo(spenderAddress)
        Truth.assertThat(actual?.amount).isNull()
    }

    @Test
    fun decodeApproveWithoutAddressPrefix() {
        val callData = "0x095ea7b30000000000000000000000004316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        Truth.assertThat(TronApprovalTokenCallData(callData)?.spenderAddress).isEqualTo(spenderAddress)
    }

    @Test
    fun decodeRejectsAnotherMethod() {
        val transferCallData = "0xa9059cbb0000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        Truth.assertThat(TronApprovalTokenCallData(transferCallData)).isNull()
    }

    @Test
    fun decodeRejectsMalformedData() {
        Truth.assertThat(TronApprovalTokenCallData("0x095ea7b3")).isNull()
        Truth.assertThat(TronApprovalTokenCallData("not a hex string")).isNull()
    }
}