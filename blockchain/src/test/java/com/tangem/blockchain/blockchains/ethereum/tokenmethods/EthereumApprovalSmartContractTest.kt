package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString

class EthereumApprovalSmartContractTest {

    private val signature = "0x095ea7b3".hexToBytes()
    private val spenderAddress = "0x5678901234567890123456789012345678901234"
    private val spenderData = "0000000000000000000000005678901234567890123456789012345678901234".hexToBytes()

    @Test
    fun makeLimitedApprovalContract() {
        val amount = "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes()
        val expected = signature + spenderData + amount

        val actual = ApprovalERC20TokenMethod(
            spenderAddress = spenderAddress,
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun makeUnlimitedApprovalContract() {
        val amount = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".hexToBytes()
        val expected = signature + spenderData + amount

        val actual = ApprovalERC20TokenMethod(
            spenderAddress = spenderAddress,
            amount = null,
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }
}