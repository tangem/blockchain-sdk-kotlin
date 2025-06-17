package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ReadEthereumAddressEIP137CallData
import com.tangem.common.extensions.hexToBytes
import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlinx.coroutines.test.runTest

class ENSRequestTest {

    @Test
    fun test1() = runTest {
        val callData = ReadEthereumAddressEIP137CallData(
            nameBytes = "07766974616c696b0365746800".hexToBytes(),
            callDataBytes = "3b3b57de".hexToBytes() +
                "ee6c4522aab0003e8d14cd40a6af439055fd2577951148c14b6cea9a53475835".hexToBytes(),
        )

        assertEquals(
            callData.dataHex,
            "0x9061b923" + // method name
                "0000000000000000000000000000000000000000000000000000000000000040" + // 64 bytes -> name bytes offset (dns encoded)
                "0000000000000000000000000000000000000000000000000000000000000080" + // 128 байт ->  call data bytes offset
                "000000000000000000000000000000000000000000000000000000000000000d" + // name bytes length
                "07766974616c696b036574680000000000000000000000000000000000000000" + // name bytes
                "0000000000000000000000000000000000000000000000000000000000000024" + // call data bytes length
                "3b3b57deee6c4522aab0003e8d14cd40a6af439055fd2577951148c14b6cea9a53475835", // call data bytes
        )

        println(callData.dataHex)
    }
}