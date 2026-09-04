package com.tangem.blockchain.collectibles.providers.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.collectibles.providers.solana.model.GachaConfigParser
import org.junit.Test
import java.util.Base64

/**
 * Parses the real `Config` account of the devnet deployment, `53EDbBLB1sQuVv6aH7us9TQ7XSGXFijPmPdAGNcY8Y1d`.
 */
internal class GachaConfigParserTest {

    private val data = Base64.getDecoder().decode(
        "mwyq4B76zIKtbdnm3fQfKiZwulw19xJe+Y62TcL9MF5YyYuupu7XZOkoOVUJZf/U1krKr0bUXfcxjltPV8kMSH1gYl2Cm4N7" +
            "hqQIUEd4g/rZZ93XJGmfY1Im7dYaVhpfB0GPFwNaf39/LRy7KpYTz3xzEH7Z5ZuyXLdQy82fbyK8F4zfpFdYmoNiez2h0gai" +
            "djS66O7bgD1GanQ/NMIfiiDM0v1mCX+YZAAAAQH/",
    )

    @Test
    fun `the devnet config account is parsed field by field`() {
        val config = requireNotNull(GachaConfigParser.parse(data))

        assertThat(config.admin).isEqualTo("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        // Collector Crypt's own mint, deliberately not Circle's USDC.
        assertThat(config.usdcMint).isEqualTo("Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr")
        assertThat(config.gachaWallet).isEqualTo("A4ahkivAG4NoZAE8Sy4qv8nn2DU9yoXRQcttuCeGtTJv")
        assertThat(config.gachaUsdcAccount).isEqualTo("9ZSgA3PMjeAU8K6CWzgJ8oDnhZwwSgHZu6KP3X95t7Jq")
        assertThat(config.feeUsdcAccount).isEqualTo("9qsWLjBgicHNncaM1qtLb4jqj4a4NixMwvNXNmsqQyV9")
        assertThat(config.feeBps).isEqualTo(100)
        assertThat(config.bump).isEqualTo(255)
    }

    @Test
    fun `the legacy filler byte is not mistaken for a flag`() {
        // Byte 171 holds a 1 on this account. Reading it as a field, or walking the tail from it, would report
        // isPaused = true and isBuybackDelegationAllowed = false — the exact opposite of the truth, and silently so.
        assertThat(data[LEGACY_FILLER_OFFSET].toInt()).isEqualTo(1)

        val config = requireNotNull(GachaConfigParser.parse(data))

        assertThat(config.isPaused).isFalse()
        assertThat(config.isBuybackDelegationAllowed).isTrue()
    }

    @Test
    fun `an account of another type is rejected`() {
        // A Vault account read at the config address would otherwise be parsed into plausible-looking addresses.
        val vaultDiscriminator = GachaVaultProgram.discriminator(211, 8, 232, 43, 2, 152, 117, 119)
        val foreign = vaultDiscriminator + data.copyOfRange(vaultDiscriminator.size, data.size)

        assertThat(GachaConfigParser.parse(foreign)).isNull()
    }

    @Test
    fun `truncated data is rejected`() {
        // One byte short is enough: the missing field would be the bump.
        assertThat(GachaConfigParser.parse(data.copyOf(data.size - 1))).isNull()
        assertThat(GachaConfigParser.parse(ByteArray(0))).isNull()
    }

    private companion object {
        const val LEGACY_FILLER_OFFSET = 171
    }
}