package com.tangem.blockchain.blockchains.icp

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.TrustWalletAddressService
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Test
import wallet.core.jni.CoinType
import wallet.core.jni.PrivateKey

class InternetComputerAddressTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val addressService = TrustWalletAddressService(Blockchain.InternetComputer)
    private val coinType = CoinType.INTERNETCOMPUTER

    @Test
    fun test() {
        val privateKey = PrivateKey("ee42eaada903e20ef6e5069f0428d552475c1ea7ed940842da6448f6ef9d48e7".hexToBytes())
        val publicKey = privateKey.getPublicKey(coinType)
        val address = addressService.makeAddress(walletPublicKey = publicKey.data())

        Truth.assertThat(publicKey.data().toHexString().lowercase())
            .isEqualTo(
                "048542e6fb4b17d6dfcac3948fe412c00d626728815ee7cc70509603f1bc92128a6e7548f3432d6248bc49ff44a1e50f6389238468d17f7d7024de5be9b181dbc8",
            )
        Truth.assertThat(address).isEqualTo("2f25874478d06cf68b9833524a6390d0ba69c566b02f46626979a3d6a4153211")
    }
}