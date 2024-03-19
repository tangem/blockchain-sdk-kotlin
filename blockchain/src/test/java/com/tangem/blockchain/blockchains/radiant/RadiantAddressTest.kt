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
}