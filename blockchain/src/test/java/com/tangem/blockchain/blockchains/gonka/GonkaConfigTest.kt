package com.tangem.blockchain.blockchains.gonka

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeeSelectionState
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.derivation.DerivationConfig
import com.tangem.blockchain.common.derivation.DerivationConfigV1
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.blockchain.common.derivation.DerivationConfigV3
import com.tangem.crypto.hdWallet.DerivationPath
import org.junit.Test
import java.math.BigDecimal

/**
 * JNI-free guardrails for the Gonka integration. Address generation itself relies on wallet.core
 * (TrustWallet JNI) and is covered by manual verification, mirroring the other WalletCore-based
 * Cosmos chains that also have no JVM address tests.
 */
internal class GonkaConfigTest {

    @Test
    fun `GIVEN Gonka WHEN decimals THEN 9 (ngonka is nano)`() {
        assertThat(Blockchain.Gonka.decimals()).isEqualTo(9)
    }

    @Test
    fun `GIVEN each derivation config WHEN derivations for Gonka THEN coin type 1200`() {
        val expected = DerivationPath("m/44'/1200'/0'/0/0")
        val configs = listOf<DerivationConfig>(DerivationConfigV1, DerivationConfigV2, DerivationConfigV3)

        configs.forEach { config ->
            assertThat(config.derivations(Blockchain.Gonka)[AddressType.Default]).isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN Gonka chain WHEN inspected THEN mainnet ngonka with no fees`() {
        val chain = CosmosChain.Gonka

        assertThat(chain.blockchain).isEqualTo(Blockchain.Gonka)
        assertThat(chain.chainId).isEqualTo("gonka-mainnet")
        assertThat(chain.smallestDenomination).isEqualTo("ngonka")
        assertThat(chain.allowsFeeSelection).isEqualTo(FeeSelectionState.Forbids)
        assertThat(chain.gasPrices(AmountType.Coin)).containsExactly(BigDecimal.ZERO)
    }
}