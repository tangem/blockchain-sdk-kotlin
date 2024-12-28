package com.tangem.blockchain.blockchains.ton

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.WalletCoreAddressService
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class TonAddressTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val publicKey = "993E12F27E3EA3BC0FDDA4DD09664C52FEDDC0A1191675089B7B59BACE4C1AE1".hexToBytes()
    private val tonAddressService = WalletCoreAddressService(Blockchain.TON)

    @Test
    fun checkGeneratingBounceableAddress() {
        val bounceAbleAddress = tonAddressService.makeAddress(walletPublicKey = publicKey)
        val expectedAddress = "EQC1dizHG_5rmYUJ7KkysiSZA-cQVJxxNVQqf73cvMV7obwG"
        Truth.assertThat(bounceAbleAddress).isNotEqualTo(expectedAddress)
    }

    @Test
    fun checkGeneratingNonBounceableAddress() {
        val bounceAbleAddress = tonAddressService.makeAddress(walletPublicKey = publicKey)
        val expectedAddress = "UQC1dizHG_5rmYUJ7KkysiSZA-cQVJxxNVQqf73cvMV7oeHD"
        Truth.assertThat(bounceAbleAddress).isEqualTo(expectedAddress)
    }
}