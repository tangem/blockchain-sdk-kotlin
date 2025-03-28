package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
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

        val actual = AllowanceERC20TokenMethod(
            ownerAddress = ownerAddress,
            spenderAddress = spenderAddress,
        )

        Truth.assertThat(actual.dataHex).isEqualTo(expected.toHexString())
        Truth.assertThat(actual.data).isEqualTo(expected)
    }
}