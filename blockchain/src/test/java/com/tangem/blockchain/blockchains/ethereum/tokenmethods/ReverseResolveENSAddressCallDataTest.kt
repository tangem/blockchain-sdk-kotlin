package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class ReverseResolveENSAddressCallDataTest {

    @Test
    fun testReverseResolveENSAddressCallData() {
        val address = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        val addressBytes = address.hexToBytes()
        val callData = ReverseResolveENSAddressCallData(
            address = addressBytes,
            coinType = 60,
        )
        val expectedHex = "0x5d78a217" +
            "0000000000000000000000000000000000000000000000000000000000000040" +
            "000000000000000000000000000000000000000000000000000000000000003c" +
            "0000000000000000000000000000000000000000000000000000000000000014" +
            "d8da6bf26964af9d7eed9e03e53415d37aa96045"
        Truth.assertThat(callData.dataHex).isEqualTo(expectedHex)
        Truth.assertThat(callData.methodId).isEqualTo("0x5d78a217")
    }

    @Test
    fun validateReverseResolveENSAddressCallData() {
        val address = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        val addressBytes = address.hexToBytes()
        val validCallData = ReverseResolveENSAddressCallData(
            address = addressBytes,
            coinType = 60,
        )
        Truth.assertThat(validCallData.validate()).isTrue()

        val invalidCallData = ReverseResolveENSAddressCallData(
            address = ByteArray(0),
            coinType = 60,
        )
        Truth.assertThat(invalidCallData.validate()).isFalse()
    }
}