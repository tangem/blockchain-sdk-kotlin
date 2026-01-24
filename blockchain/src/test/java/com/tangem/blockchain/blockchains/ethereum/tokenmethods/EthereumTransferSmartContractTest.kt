package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString

class EthereumTransferSmartContractTest {
    private val blockchain = Blockchain.Ethereum

    private val signature = "0xa9059cbb".hexToBytes()
    private val destinationAddress = "0x5678901234567890123456789012345678901234"
    private val destinationData = "0x0000000000000000000000005678901234567890123456789012345678901234".hexToBytes()

    @Test
    fun makeTransferContract() {
        val amount = "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes()
        val expected = signature + destinationData + amount

        val actual = TransferERC20TokenCallData(
            destination = destinationAddress,
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun validateTransferContract() {
        val validContract = TransferERC20TokenCallData(
            destination = destinationAddress,
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )
        Truth.assertThat(validContract.validate(blockchain)).isTrue()

        val invalidContract = TransferERC20TokenCallData(
            destination = "",
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )
        Truth.assertThat(invalidContract.validate(blockchain)).isFalse()

        val invalidContract2 = TransferERC20TokenCallData(
            destination = EthereumUtils.ZERO_ADDRESS,
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )
        Truth.assertThat(invalidContract2.validate(blockchain)).isFalse()

        val invalidContract3 = TransferERC20TokenCallData(
            destination = "0xG234567890123456789012345678901234567890",
            amount = Amount(Blockchain.Ethereum).copy(
                value = "100".toBigDecimal(),
            ),
        )
        Truth.assertThat(invalidContract3.validate(blockchain)).isFalse()
    }
}