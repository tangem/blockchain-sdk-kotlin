package com.tangem.blockchain.blockchains.koinos

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.derivation.DerivationConfigV1
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.blockchain.common.derivation.DerivationConfigV3
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import org.junit.Test

class KoinosConstantsTest {

    private val decimals = 8
    private val derivations = mapOf(AddressType.Default to DerivationPath("m/44'/659'/0'/0/0"))
    private val supportedCurves = listOf(EllipticCurve.Secp256k1)
    private val blockchain = Blockchain.Koinos
    private val blockchainTestnet = Blockchain.KoinosTestnet

    @Test
    fun correctDerivationPathConfigV1() {
        Truth.assertThat(DerivationConfigV1.derivations(blockchain))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV2() {
        Truth.assertThat(DerivationConfigV2.derivations(blockchain))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV3() {
        Truth.assertThat(DerivationConfigV3.derivations(blockchain))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDecimals() {
        Truth.assertThat(blockchain.decimals()).isEqualTo(decimals)
    }

    @Test
    fun correctCurve() {
        Truth.assertThat(blockchain.getSupportedCurves())
            .isEqualTo(supportedCurves)
    }

    @Test
    fun correctDerivationPathConfigV1Testnet() {
        Truth.assertThat(DerivationConfigV1.derivations(blockchainTestnet))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV2Testnet() {
        Truth.assertThat(DerivationConfigV2.derivations(blockchainTestnet))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV3Testnet() {
        Truth.assertThat(DerivationConfigV3.derivations(blockchainTestnet))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDecimalsTestnet() {
        Truth.assertThat(blockchainTestnet.decimals()).isEqualTo(decimals)
    }

    @Test
    fun correctCurveTestnet() {
        Truth.assertThat(blockchainTestnet.getSupportedCurves())
            .isEqualTo(supportedCurves)
    }
}