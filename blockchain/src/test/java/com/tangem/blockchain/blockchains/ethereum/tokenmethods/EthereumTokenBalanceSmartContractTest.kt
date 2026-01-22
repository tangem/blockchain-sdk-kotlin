package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString

class EthereumTokenBalanceSmartContractTest {

    private val signature = "0x70a08231".hexToBytes()
    private val address = "0x1234567890123456789012345678901234567890"
    private val addressData = "0000000000000000000000001234567890123456789012345678901234567890".hexToBytes()

    @Test
    fun makeBalanceOfContract() {
        val expected = signature + addressData

        val actual = TokenBalanceERC20TokenCallData(address = address)

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun validateBalanceOfContract() {
        val validContract = TokenBalanceERC20TokenCallData(address = address)
        Truth.assertThat(validContract.validate()).isTrue()

        val invalidContract = TokenBalanceERC20TokenCallData(address = "")
        Truth.assertThat(invalidContract.validate()).isFalse()

        val invalidContract1 = TokenBalanceERC20TokenCallData(address = EthereumUtils.ZERO_ADDRESS)
        Truth.assertThat(invalidContract1.validate()).isFalse()

        val invalidContract2 = TokenBalanceERC20TokenCallData(address = "0xG234567890123456789012345678901234567890")
        Truth.assertThat(invalidContract2.validate()).isFalse()
    }
}