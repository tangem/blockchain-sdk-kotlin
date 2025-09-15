package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.converters.ENSReverseResponseConverter
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ENSReverseResponseTest {

    @Test
    fun testReverseResolveENSAddressResponse() {
        val result = "0x" +
            "0000000000000000000000000000000000000000000000000000000000000060" + // offset to bytes
            "000000000000000000000000231b0ee14048e9dccd1d247744d114a4eb5e8e63" + // forward resolver address
            "0000000000000000000000005fbb459c49bb06083c33109fa4f14810ec2cf358" + // reverse resolver address
            "000000000000000000000000000000000000000000000000000000000000000b" + // length of ens name bytes
            "766974616c696b2e657468000000000000000000000000000000000000000000" // ens name bytes

        val expected = "vitalik.eth"
        val actual = ENSReverseResponseConverter.convert(result)
        assertEquals(expected, actual)
    }
}