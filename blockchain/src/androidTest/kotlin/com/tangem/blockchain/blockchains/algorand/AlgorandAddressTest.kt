package com.tangem.blockchain.blockchains.algorand

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.WalletCoreAddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class AlgorandAddressTest {

    private val addressService = WalletCoreAddressService(Blockchain.Algorand)

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Test
    fun test() {
        val walletPublicKey = "67CFA0C50B5A46A3FF6FD38CB6D6E45725EAC937A79E3528A13A71BC006F877E".hexToBytes()
        val expected = "M7H2BRILLJDKH73P2OGLNVXEK4S6VSJXU6PDKKFBHJY3YADPQ57HN4EI64"

        Truth.assertThat(addressService.makeAddress(walletPublicKey, EllipticCurve.Ed25519)).isEqualTo(expected)
    }
}