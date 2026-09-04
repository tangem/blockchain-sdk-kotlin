package com.tangem.blockchain.collectibles.providers.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.collectibles.providers.solana.model.GachaVaultParser
import org.junit.Test
import org.p2p.solanaj.core.PublicKey
import java.util.Base64

/**
 * Parses the real `Vault` account of the devnet deployment, `aBRHcE97NY1MjVqGbc2LX2cApfPg9r5h5T1KNZS93oA`.
 *
 * The account is worth keeping as a fixture: both `Option<Pubkey>` fields are `None`, yet the 65 bytes behind them
 * still hold the pubkeys of a buyback that has since been closed. That is the layout a fixed-offset reader gets
 * wrong.
 */
internal class GachaVaultParserTest {

    private val address = PublicKey("aBRHcE97NY1MjVqGbc2LX2cApfPg9r5h5T1KNZS93oA")

    private val data = Base64.getDecoder().decode(
        "0wjoKwKYdXetbdnm3fQfKiZwulw19xJe+Y62TcL9MF5YyYuupu7XZK1t2ebd9B8qJnC6XDX3El75jrZNwv0wXljJi66m7tdk" +
            "AKPhEQAAAAAAypo7AAAAAMDV6xQAAAAAq1AAAAAAAAAAAAAAAAAAAAAA+ppGQwvkMDJMqMYTcIxKXkxjJtBwIEbt9t5vis64" +
            "qwFVhTe0bFNh/9snI5gQE/nRFZ19QIb6mKkJhaNYjyxZ3Po=",
    )

    @Test
    fun `the devnet vault account is parsed field by field`() {
        // Why this dump: both option tags are None, yet the bytes behind them are not zeroed on close. On an
        // account with a clean tail the isNull assertions below would pass for any parser.
        assertThat(data[TAIL_OFFSET].toInt()).isEqualTo(0)
        assertThat(data[TAIL_OFFSET + 1].toInt()).isEqualTo(0)
        assertThat(data.copyOfRange(TAIL_OFFSET + 2, data.size).all { it == 0.toByte() }).isFalse()

        val vault = requireNotNull(GachaVaultParser.parse(address, data))

        assertThat(vault.coldOwner).isEqualTo("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        // This vault delegates to the cold key itself, so the two addresses being equal is the account, not a
        // parser reading the same 32 bytes twice — the fields sit at offsets 8 and 40.
        assertThat(vault.hotDelegate).isEqualTo("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        assertThat(vault.perSpinCap).isEqualTo(300_000_000L)
        assertThat(vault.dailyCap).isEqualTo(1_000_000_000L)
        assertThat(vault.spentToday).isEqualTo(351_000_000L)
        assertThat(vault.dayIndex).isEqualTo(20_651L)
        // Reading the pubkey at a fixed offset behind the tag would hand the caller
        // 14pdnavX9L9iMbBYpv3M6W6iVqYo3Cy3vAzVRVc9gLee — a plausible address, and a buyback that does not exist.
        assertThat(vault.liveBuybackMint).isNull()
        assertThat(vault.liveBuybackToken).isNull()
    }

    /**
     * Synthetic, unlike the rest of the class: no vault with a live buyback exists on devnet, so the `Some` branch is
     * reached by flipping the first option tag of the real account by hand. That one byte shifts the whole tail, and
     * the second tag then has to be found at 145 — where the closed buyback's own tag is still sitting.
     *
     * The expected addresses are those stale bytes, not meaningful pubkeys: the assertion is about cursor arithmetic.
     */
    @Test
    fun `a live buyback is read when the first option tag is set`() {
        val withBuyback = data.copyOf().also { it[TAIL_OFFSET] = 1 }

        val vault = requireNotNull(GachaVaultParser.parse(address, withBuyback))

        assertThat(vault.liveBuybackMint).isEqualTo("14pdnavX9L9iMbBYpv3M6W6iVqYo3Cy3vAzVRVc9gLee")
        assertThat(vault.liveBuybackToken).isEqualTo("6kqUQvKm5GArcgLPokDjj4XK6rE1wR1EKqEptJuroFZd")
    }

    @Test
    fun `an account of another type is rejected`() {
        // A Config account read at the vault address would otherwise be parsed into plausible-looking addresses.
        val configDiscriminator = GachaVaultProgram.discriminator(155, 12, 170, 224, 30, 250, 204, 130)
        val foreign = configDiscriminator + data.copyOfRange(configDiscriminator.size, data.size)

        assertThat(GachaVaultParser.parse(address, foreign)).isNull()
    }

    @Test
    fun `truncated data is rejected`() {
        // Cut back to the last fixed field: the options are gone, so nothing can be said about the buyback.
        assertThat(GachaVaultParser.parse(address, data.copyOf(TAIL_OFFSET))).isNull()
        assertThat(GachaVaultParser.parse(address, ByteArray(0))).isNull()
    }

    @Test
    fun `a truncated some option is rejected`() {
        val truncatedBuyback = data.copyOf(TAIL_OFFSET + 10).also { it[TAIL_OFFSET] = 1 }

        assertThat(GachaVaultParser.parse(address, truncatedBuyback)).isNull()
    }

    private companion object {
        const val TAIL_OFFSET = 112
    }
}