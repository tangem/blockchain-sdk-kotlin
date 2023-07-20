package com.tangem.blockchain.blockchains.tezos

import com.google.common.truth.Truth
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class TezosAddressTest {

    private val addressService = TezosAddressService(EllipticCurve.Ed25519)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "98E0E504F3A5FDE704400302ABB0A2EFB0DF0F95C166C91D7F207DEDCE10CBA3"
                .hexToBytes()
        val expected = "tz1hhRdWDAvGsgEioZ9GAp4bUVQkd9ng2MMR"

        Truth.assertThat(addressService.makeAddressWithDefaultType(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "tz1hhRdWDAvGsgEioZ9GAp4bUVQkd9ng2MMR"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}