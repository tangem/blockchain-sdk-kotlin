package com.tangem.blockchain.blockchains.tron

import com.google.common.truth.Truth
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class TronAddressTest {
    private val addressService = TronAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "0404B604296010A55D40000B798EE8454ECCC1F8900E70B1ADF47C9887625D8BAE3866351A6FA0B5370623268410D33D345F63344121455849C9C28F9389ED9731"
            .hexToBytes()
        val expected = "TDpBe64DqirkKWj6HWuR1pWgmnhw2wDacE"

        Truth.assertThat(addressService.makeAddressWithDefaultType(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun makeAddressFromCompressedPublicKey() {
        val walletPublicKey = "034c88a1a83469ddf20d0c07e5c4a1e7b83734e721e60d642b94a53222c47c670d"
            .hexToBytes()
        val expected = "TF2cog7GWQ4abn2UGP87xqZW7uy1KPfvMa"

        Truth.assertThat(addressService.makeAddressWithDefaultType(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress_returnsTrue() {
        Truth.assertThat(
            addressService.validate("TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        ).isTrue()
        Truth.assertThat(
            addressService.validate("TDpBe64DqirkKWj6HWuR1pWgmnhw2wDacE")
        ).isTrue()
    }

    @Test
    fun validateIncorrectAddress_returnsFalse() {
        Truth.assertThat(
            addressService.validate("RJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        ).isFalse()
    }
}