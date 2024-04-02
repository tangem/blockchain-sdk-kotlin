package com.tangem.blockchain.blockchains.radiant

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class RadiantAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Radiant)

    @Test
    fun makeP2pkhAddressFromCorrectPublicKey() {
        val walletPublicKey = "039d645d2ce630c2a9a6dbe0cbd0a8fcb7b70241cb8a48424f25593290af2494b9".hexToBytes()

        val expectedSize = 1
        val expectedLegacyAddress = "12dNaXQtN5Asn2YFwT1cvciCrJa525fAe4"

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