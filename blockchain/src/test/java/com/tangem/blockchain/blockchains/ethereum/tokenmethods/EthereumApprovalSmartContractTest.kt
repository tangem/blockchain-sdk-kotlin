package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
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

        val actual = ApprovalERC20TokenCallData(
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

        val actual = ApprovalERC20TokenCallData(
            spenderAddress = spenderAddress,
            amount = null,
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun validateApprovalContract() {
        val validContract = ApprovalERC20TokenCallData(
            spenderAddress = spenderAddress,
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )
        Truth.assertThat(validContract.validate()).isTrue()

        val validContractUnlimited = ApprovalERC20TokenCallData(
            spenderAddress = spenderAddress,
            amount = null,
        )
        Truth.assertThat(validContractUnlimited.validate()).isTrue()

        val invalidContract = ApprovalERC20TokenCallData(
            spenderAddress = "",
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )
        Truth.assertThat(invalidContract.validate()).isFalse()

        val invalidContract1 = ApprovalERC20TokenCallData(
            spenderAddress = EthereumUtils.ZERO_ADDRESS,
            amount = null,
        )
        Truth.assertThat(invalidContract1.validate()).isFalse()

        val invalidContract2 = ApprovalERC20TokenCallData(
            spenderAddress = "0xG234567890123456789012345678901234567890",
            amount = null,
        )
        Truth.assertThat(invalidContract2.validate()).isFalse()
    }
}