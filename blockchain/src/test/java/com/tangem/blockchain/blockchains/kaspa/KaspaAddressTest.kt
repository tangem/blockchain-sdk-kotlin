package com.tangem.blockchain.blockchains.kaspa

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class KaspaAddressTest {

    private val addressService = KaspaAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey =
            "04586190332C39188029D0DA9B2DBA0605ED3E3FFCEF9C270D64FDCB8C0BA48A601E4FA5CD60AFB067B7C554284BEEE67CEAE6AB0634975E288507BD051904935F".hexToBytes()
        val expected = "kaspa:qyp4scvsxvkrjxyq98gd4xedhgrqtmf78l7wl8p8p4j0mjuvpwjg5cqhy97n472"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
            .isEqualTo(expected)
    }

    @Test
    fun validateCorrectECDSAAddress() {
        val address = "kaspa:qyp4scvsxvkrjxyq98gd4xedhgrqtmf78l7wl8p8p4j0mjuvpwjg5cqhy97n472"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectSchnorrAddress() {
        val address = "kaspa:qpsqw2aamda868dlgqczeczd28d5nc3rlrj3t87vu9q58l2tugpjs2psdm4fv"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectP2SHAddress() {
        val address = "kaspa:pqurku73qluhxrmvyj799yeyptpmsflpnc8pha80z6zjh6efwg3v2rrepjm5r"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun getCorrectECDSAAddressPublicKey() {
        val address = "kaspa:qyp4scvsxvkrjxyq98gd4xedhgrqtmf78l7wl8p8p4j0mjuvpwjg5cqhy97n472"
        val expected = "03586190332C39188029D0DA9B2DBA0605ED3E3FFCEF9C270D64FDCB8C0BA48A60".hexToBytes()

        Truth.assertThat(addressService.getPublicKey(address)).isEqualTo(expected)
    }

    @Test
    fun getCorrectSchnorrAddressPublicKey() {
        val address = "kaspa:qpsqw2aamda868dlgqczeczd28d5nc3rlrj3t87vu9q58l2tugpjs2psdm4fv"
        val expected = "60072BBDDB7A7D1DBF40302CE04D51DB49E223F8E5159FCCE14143FD4BE20328".hexToBytes()

        Truth.assertThat(addressService.getPublicKey(address)).isEqualTo(expected)
    }
}