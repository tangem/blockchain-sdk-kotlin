package com.tangem.blockchain.blockchains.kusama

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class KusamaAddressTest {

    private val kusamaAddressService = PolkadotAddressService(Blockchain.Kusama)

    private val kusamaAddress = "Dzx4cJDqQgcjkpetBcaWRV7FMCXKyHBrxFYrrD6HFNE7aj4"

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "3F05253ACDDB17A527BA6E9DBD73E7B06FFCF7CE072041052BEF31B6ECBD7CE2".hexToBytes()
        Truth.assertThat(kusamaAddressService.makeAddressWithDefaultType(walletPublicKey)).isEqualTo(kusamaAddress)
    }

    @Test
    fun validateCorrectAddress() {
        Truth.assertThat(kusamaAddressService.validate(kusamaAddress)).isTrue()
    }
}