package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString

class TronTransferSmartContractTest {

    private val signature = "0xa9059cbb".hexToBytes()
    private val destinationAddress = "TG5wUqBkukAho2E38ca3EZG4zvYp3hUivZ"
    private val destinationData = "0000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3".hexToBytes()

    @Test
    fun makeLimitedAmountApprove() {
        val amount = "000000000000000000000000000000000000000000000000000000003b9aca00".hexToBytes()
        val expected = signature + destinationData + amount

        val actual = TronTransferTokenCallData(
            destination = destinationAddress,
            amount = Amount(Blockchain.Tron).copy(
                value = "1000".toBigDecimal(),
            ),
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun decodeTransfer() {
        val callData = "0xa9059cbb0000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        val actual = TronTransferTokenCallData(callData)

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.destination).isEqualTo(destinationAddress)
        Truth.assertThat(actual.amount.value!!.compareTo("1000000000".toBigDecimal())).isEqualTo(0)
    }

    @Test
    fun decodeTransferWithoutHexPrefix() {
        val callData = "a9059cbb0000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        Truth.assertThat(TronTransferTokenCallData(callData)?.destination).isEqualTo(destinationAddress)
    }

    @Test
    fun decodeTransferWithoutAddressPrefix() {
        // A wallet that reuses the EVM encoding pads the 20-byte body, leaving the Tron prefix byte zero
        val callData = "0xa9059cbb0000000000000000000000004316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        val actual = TronTransferTokenCallData(callData)

        Truth.assertThat(actual?.destination).isEqualTo(destinationAddress)
        Truth.assertThat(actual?.amount?.value!!.compareTo("1000000000".toBigDecimal())).isEqualTo(0)
    }

    @Test
    fun decodeRejectsZeroAddress() {
        val callData = "0xa9059cbb0000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        Truth.assertThat(TronTransferTokenCallData(callData)).isNull()
    }

    @Test
    fun decodeRejectsDirtyAddressPadding() {
        val callData = "0xa9059cbb0000000000000000000100000041b5c3f99fd4818918abced9d7df3d31396ae3ff" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        Truth.assertThat(TronTransferTokenCallData(callData)).isNull()
    }

    @Test
    fun decodeRejectsAnotherMethod() {
        val approveCallData = "0x095ea7b30000000000000000000000414316b5c3f99fd4818918abced9d7df3d31396ae3" +
            "000000000000000000000000000000000000000000000000000000003b9aca00"

        Truth.assertThat(TronTransferTokenCallData(approveCallData)).isNull()
    }

    @Test
    fun decodeRejectsMalformedData() {
        Truth.assertThat(TronTransferTokenCallData("0xa9059cbb")).isNull()
        Truth.assertThat(TronTransferTokenCallData("not a hex string")).isNull()
    }
}