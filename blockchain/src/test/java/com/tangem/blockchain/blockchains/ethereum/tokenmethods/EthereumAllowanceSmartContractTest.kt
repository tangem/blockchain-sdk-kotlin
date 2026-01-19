package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.komputing.khex.extensions.toHexString

class EthereumAllowanceSmartContractTest {

    private val signature = "0xdd62ed3e".hexToBytes()
    private val ownerAddress = "0x1234567890123456789012345678901234567890"
    private val spenderAddress = "0x5678901234567890123456789012345678901234"
    private val spenderData = "0000000000000000000000005678901234567890123456789012345678901234".hexToBytes()
    private val ownerData = "0000000000000000000000001234567890123456789012345678901234567890".hexToBytes()

    @Test
    fun makeAllowanceContract() {
        val expected = signature + ownerData + spenderData

        val actual = AllowanceERC20TokenCallData(
            ownerAddress = ownerAddress,
            spenderAddress = spenderAddress,
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }

    @Test
    fun validateAllowanceContract() {
        val validContract = AllowanceERC20TokenCallData(
            ownerAddress = ownerAddress,
            spenderAddress = spenderAddress,
        )
        Truth.assertThat(validContract.validate()).isTrue()

        val invalidContract1 = AllowanceERC20TokenCallData(
            ownerAddress = "",
            spenderAddress = spenderAddress,
        )
        Truth.assertThat(invalidContract1.validate()).isFalse()

        val invalidContract2 = AllowanceERC20TokenCallData(
            ownerAddress = ownerAddress,
            spenderAddress = "",
        )
        Truth.assertThat(invalidContract2.validate()).isFalse()

        val invalidContract3 = AllowanceERC20TokenCallData(
            ownerAddress = EthereumUtils.ZERO_ADDRESS,
            spenderAddress = spenderAddress,
        )
        Truth.assertThat(invalidContract3.validate()).isFalse()

        val invalidContract4 = AllowanceERC20TokenCallData(
            ownerAddress = ownerAddress,
            spenderAddress = EthereumUtils.ZERO_ADDRESS,
        )
        Truth.assertThat(invalidContract4.validate()).isFalse()

        val invalidContract5 = AllowanceERC20TokenCallData(
            ownerAddress = "0xG234567890123456789012345678901234567890",
            spenderAddress = spenderAddress,
        )
        Truth.assertThat(invalidContract5.validate()).isFalse()

        val invalidContract6 = AllowanceERC20TokenCallData(
            ownerAddress = ownerAddress,
            spenderAddress = "0xG234567890123456789012345678901234567890",
        )
        Truth.assertThat(invalidContract6.validate()).isFalse()
    }
}