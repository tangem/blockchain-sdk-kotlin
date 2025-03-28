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

        val actual = TronTransferTokenMethod(
            destination = destinationAddress,
            amount = Amount(Blockchain.Tron).copy(
                value = "1000".toBigDecimal(),
            ),
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }
}