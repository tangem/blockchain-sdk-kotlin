package com.tangem.blockchain.blockchains.ton

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.TrustWalletAddressService
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import wallet.core.jni.TheOpenNetworkAddress

internal class TonAddressTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val blockchain = Blockchain.TON
    private val publicKey = "993E12F27E3EA3BC0FDDA4DD09664C52FEDDC0A1191675089B7B59BACE4C1AE1".hexToBytes()
    private val tonAddressService = TonAddressService(blockchain)
    private val legacyAddressService = TrustWalletAddressService(blockchain)

    @Test
    fun checkEqualityWithLegacyAddressService() {
        val legacyAddress = TheOpenNetworkAddress(legacyAddressService.makeAddress(walletPublicKey = publicKey))
        val newAddress = tonAddressService.makeTheOpenNetworkAddress(walletPublicKey = publicKey)
        Truth.assertThat(TheOpenNetworkAddress.equals(legacyAddress, newAddress)).isTrue()
    }

    @Test
    fun checkGeneratingBounceableAddress() {
        val bounceAbleAddress = tonAddressService.makeTheOpenNetworkAddress(walletPublicKey = publicKey)
        val expectedAddress = "EQC1dizHG_5rmYUJ7KkysiSZA-cQVJxxNVQqf73cvMV7obwG"
        Truth.assertThat(
            bounceAbleAddress.stringRepresentation(/*userFriendly*/ true, /*bounceable*/ true, /*testOnly*/ false),
        ).isEqualTo(expectedAddress)
    }

    @Test
    fun checkGeneratingNonBounceableAddress() {
        val bounceAbleAddress = tonAddressService.makeTheOpenNetworkAddress(walletPublicKey = publicKey)
        val expectedAddress = "UQC1dizHG_5rmYUJ7KkysiSZA-cQVJxxNVQqf73cvMV7oeHD"
        Truth.assertThat(
            bounceAbleAddress.stringRepresentation(/*userFriendly*/ true, /*bounceable*/ false, /*testOnly*/ false),
        ).isEqualTo(expectedAddress)
    }

    @Test
    fun checkRawForm() {
        val bounceAbleAddress = tonAddressService.makeTheOpenNetworkAddress(walletPublicKey = publicKey)
        val expectedAddress = "0:b5762cc71bfe6b998509eca932b2249903e710549c7135542a7fbddcbcc57ba1"
        Truth.assertThat(
            bounceAbleAddress.stringRepresentation(/*userFriendly*/ false, /*bounceable*/ false, /*testOnly*/ false),
        ).isEqualTo(expectedAddress)
    }

    @Test
    fun addressServiceGeneratingCorrectAddress() {
        val theOpenNetworkAddress = tonAddressService.makeTheOpenNetworkAddress(walletPublicKey = publicKey)
        val generatedAddress = tonAddressService.makeAddress(walletPublicKey = publicKey)
        Truth.assertThat(generatedAddress)
            .isEqualTo(
                theOpenNetworkAddress.stringRepresentation(
                    /*userFriendly*/ true,
                    /*bounceable*/ false,
                    /*testOnly*/ false,
                ),
            )
    }
}