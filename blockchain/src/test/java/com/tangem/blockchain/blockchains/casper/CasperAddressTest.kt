package com.tangem.blockchain.blockchains.casper

import com.google.common.truth.Truth
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class CasperAddressTest {

    private val addressService = CasperAddressService()

    @Test
    fun makeAddressFromCorrectEd25519PublicKey() {
        val walletPublicKey = "98C07D7E72D89A681D7227A7AF8A6FD5F22FE0105C8741D55A95DF415454B82E".hexToBytes()
        val expected = "0198c07D7e72D89A681d7227a7Af8A6fd5F22fe0105c8741d55A95dF415454b82E"

        Truth.assertThat(addressService.makeAddress(walletPublicKey, EllipticCurve.Ed25519)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectEd25519Address() {
        val address = "0198c07D7e72D89A681d7227a7Af8A6fd5F22fe0105c8741d55A95dF415454b82E"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun makeAddressFromCorrectSecp256k1PublicKey() {
        val walletPublicKey = "021F997DFBBFD32817C0E110EAEE26BCBD2BB70B4640C515D9721C9664312EACD8".hexToBytes()
        val expected = "02021f997DfbbFd32817C0E110EAeE26BCbD2BB70b4640C515D9721c9664312eaCd8"

        Truth.assertThat(addressService.makeAddress(walletPublicKey, EllipticCurve.Secp256k1)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectSecp256k1Address() {
        val address = "02021f997DfbbFd32817C0E110EAeE26BCbD2BB70b4640C515D9721c9664312eaCd8"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}