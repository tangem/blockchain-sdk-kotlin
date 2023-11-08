package com.tangem.blockchain.blockchains.kaspa

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class KaspaAddressTest {

    private val addressService = KaspaAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey =
            "04586190332C39188029D0DA9B2DBA0605ED3E3FFCEF9C270D64FDCB8C0BA48A601E4FA5CD60AFB067B7C554284BEEE67CEAE6AB0634975E288507BD051904935F".hexToBytes()
        val expected = "kaspa:qyp4scvsxvkrjxyq98gd4xedhgrqtmf78l7wl8p8p4j0mjuvpwjg5cqhy97n472"

        Truth.assertThat(addressService.makeAddressWithDefaultType(walletPublicKey))
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

    @Test
    fun testKaspaAddressGeneration() {
        // https://github.com/kaspanet/kaspad/pull/2202/files
        // https://github.com/kaspanet/kaspad/blob/dev/util/address_test.go
        val address = "kaspa:qyp0r5mcq4rd5grj3652ra09u5dcgwqq9ntuswp247nama5quyj40eq03sc2dkx"
        val publicKey = byteArrayOf(
            // region Bytes
            0x02.toByte(),
            0xf1.toByte(),
            0xd3.toByte(),
            0x78.toByte(),
            0x05.toByte(),
            0x46.toByte(),
            0xda.toByte(),
            0x20.toByte(),
            0x72.toByte(),
            0x8e.toByte(),
            0xa8.toByte(),
            0xa1.toByte(),
            0xf5.toByte(),
            0xe5.toByte(),
            0xe5.toByte(),
            0x1b.toByte(),
            0x84.toByte(),
            0x38.toByte(),
            0x00.toByte(),
            0x2c.toByte(),
            0xd7.toByte(),
            0xc8.toByte(),
            0x38.toByte(),
            0x2a.toByte(),
            0xaf.toByte(),
            0xa7.toByte(),
            0xdd.toByte(),
            0xf6.toByte(),
            0x80.toByte(),
            0xe1.toByte(),
            0x25.toByte(),
            0x57.toByte(),
            0xe4.toByte(),
            // endregion Bytes
        )

        Truth.assertThat(addressService.makeAddressWithDefaultType(publicKey)).isEqualTo(address)
    }
}
