package com.tangem.blockchain.blockchains.radiant

import com.google.common.truth.Truth
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class RadiantAddressTest {

    private val addressService = RadiantAddressService()

    @Test
    fun makeP2pkhAddressFromCorrectPublicKey() {
        val walletPublicKey = "0441DCD64B5F4A039FC339A16300A833A883B218909F2EBCAF3906651C76842C45E3D67E8D2947E6FEE8B62D3D3B6A4D5F212DA23E478DD69A2C6CCC851F300D80".hexToBytes()

        val expectedSize = 1
        val expectedLegacyAddress = "1JjXGY5KEcbT35uAo6P9A7DebBn4DXnjdQ"

        val addresses = addressService.makeAddresses(walletPublicKey)
        val legacyAddress = requireNotNull(addresses.find { it.type == AddressType.Default })

        Truth.assertThat(addresses.size).isEqualTo(expectedSize)
        Truth.assertThat(legacyAddress.value).isEqualTo(expectedLegacyAddress)
    }

    @Test
    fun testGeneratingScriptHash() {
        val scriptHash = RadiantAddressUtils.generateAddressScriptHash(
            walletAddress = "1vr9gJkNzTHv8DEQb4QBxAnQCxgzkFkbf",
        )
        Truth.assertThat("972C432D04BC6908FA2825860148B8F911AC3D19C161C68E7A6B9BEAE86E05BA").isEqualTo(scriptHash)

        val scriptHash1 = RadiantAddressUtils.generateAddressScriptHash(
            walletAddress = "166w5AGDyvMkJqfDAtLbTJeoQh6FqYCfLQ",
        )
        Truth.assertThat("67809980FB38F7685D46A8108A39FE38956ADE259BE1C3E6FECBDEAA20FDECA9").isEqualTo(scriptHash1)
    }
}