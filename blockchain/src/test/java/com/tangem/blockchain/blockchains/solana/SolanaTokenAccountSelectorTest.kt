package com.tangem.blockchain.blockchains.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.model.NewSolanaTokenAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaTokenAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import org.junit.Test
import org.p2p.solanaj.core.PublicKey
import java.math.BigDecimal

internal class SolanaTokenAccountSelectorTest {

    private val owner = "Cw3YcfqzRSa7xT7ecpR5E4FKDQU6aaxz5cWje366CZbf"
    private val mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
    private val anotherMint = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
    private val nonAssociatedAddress = "BmAzxn8WLYU3gEw79ATUdSUkMT53MeS5LjapBQB8gTPJ"
    private val anotherNonAssociatedAddress = "2gVkYWexTHR5Hb2aLeQN3tnngvWzisFKXDUPrgMHpdST"

    @Test
    fun `GIVEN associated account is not first WHEN uniqueAccountsByMint THEN associated account is selected`() {
        // Arrange
        val associatedAddress = associatedTokenAddress(mint = mint)
        val accounts = listOf(
            account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(100)),
            account(address = associatedAddress, mint = mint, solAmount = BigDecimal.ONE),
        )

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[1])
    }

    @Test
    fun `GIVEN associated account is first WHEN uniqueAccountsByMint THEN associated account is selected`() {
        // Arrange
        val associatedAddress = associatedTokenAddress(mint = mint)
        val accounts = listOf(
            account(address = associatedAddress, mint = mint, solAmount = BigDecimal.ONE),
            account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(100)),
        )

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[0])
    }

    @Test
    fun `GIVEN token2022 associated account WHEN uniqueAccountsByMint THEN associated account is selected`() {
        // Arrange
        val associatedAddress = associatedTokenAddress(mint = mint, programId = SolanaTokenProgram.ID.TOKEN_2022)
        val accounts = listOf(
            account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(50)),
            account(address = associatedAddress, mint = mint, solAmount = BigDecimal(7)),
        )

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[1])
    }

    @Test
    fun `GIVEN empty associated account WHEN uniqueAccountsByMint THEN it is still selected over a funded one`() {
        // Arrange
        val associatedAddress = associatedTokenAddress(mint = mint)
        val accounts = listOf(
            account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(100)),
            account(address = associatedAddress, mint = mint, solAmount = BigDecimal.ZERO),
        )

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[1])
    }

    @Test
    fun `GIVEN single account WHEN uniqueAccountsByMint THEN it is selected regardless of being associated`() {
        // Arrange
        val accounts = listOf(account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(3)))

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[0])
    }

    @Test
    fun `GIVEN no associated account WHEN uniqueAccountsByMint THEN first account is selected`() {
        // Arrange
        val accounts = listOf(
            account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(3)),
            account(address = anotherNonAssociatedAddress, mint = mint, solAmount = BigDecimal(9)),
        )

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[0])
    }

    @Test
    fun `GIVEN accounts of two mints WHEN uniqueAccountsByMint THEN each mint resolves its own associated account`() {
        // Arrange
        val accounts = listOf(
            account(address = nonAssociatedAddress, mint = mint, solAmount = BigDecimal(40)),
            account(address = associatedTokenAddress(mint = mint), mint = mint, solAmount = BigDecimal(2)),
            account(address = anotherNonAssociatedAddress, mint = anotherMint, solAmount = BigDecimal(80)),
            account(
                address = associatedTokenAddress(mint = anotherMint),
                mint = anotherMint,
                solAmount = BigDecimal(5),
            ),
        )

        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = accounts, ownerAccountId = owner)

        // Assert
        assertThat(actual).containsExactly(mint, accounts[1], anotherMint, accounts[3])
    }

    @Test
    fun `GIVEN no accounts WHEN uniqueAccountsByMint THEN result is empty`() {
        // Act
        val actual = SolanaTokenAccountSelector.uniqueAccountsByMint(accounts = emptyList(), ownerAccountId = owner)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN associated address is not first WHEN preferredAccountAddress THEN associated address is returned`() {
        // Arrange
        val associatedAddress = associatedTokenAddress(mint = mint)

        // Act
        val actual = SolanaTokenAccountSelector.preferredAccountAddress(
            addresses = listOf(nonAssociatedAddress, associatedAddress),
            mint = mint,
            ownerAccountId = owner,
        )

        // Assert
        assertThat(actual).isEqualTo(associatedAddress)
    }

    @Test
    fun `GIVEN token2022 associated address WHEN preferredAccountAddress THEN associated address is returned`() {
        // Arrange
        val associatedAddress = associatedTokenAddress(mint = mint, programId = SolanaTokenProgram.ID.TOKEN_2022)

        // Act
        val actual = SolanaTokenAccountSelector.preferredAccountAddress(
            addresses = listOf(nonAssociatedAddress, associatedAddress),
            mint = mint,
            ownerAccountId = owner,
        )

        // Assert
        assertThat(actual).isEqualTo(associatedAddress)
    }

    @Test
    fun `GIVEN no associated address WHEN preferredAccountAddress THEN first address is returned`() {
        // Act
        val actual = SolanaTokenAccountSelector.preferredAccountAddress(
            addresses = listOf(nonAssociatedAddress, anotherNonAssociatedAddress),
            mint = mint,
            ownerAccountId = owner,
        )

        // Assert
        assertThat(actual).isEqualTo(nonAssociatedAddress)
    }

    @Test
    fun `GIVEN no addresses WHEN preferredAccountAddress THEN null is returned`() {
        // Act
        val actual = SolanaTokenAccountSelector.preferredAccountAddress(
            addresses = emptyList(),
            mint = mint,
            ownerAccountId = owner,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN unparsable owner WHEN preferredAccountAddress THEN first address is returned`() {
        // Act
        val actual = SolanaTokenAccountSelector.preferredAccountAddress(
            addresses = listOf(nonAssociatedAddress, associatedTokenAddress(mint = mint)),
            mint = mint,
            ownerAccountId = "not-a-public-key",
        )

        // Assert
        assertThat(actual).isEqualTo(nonAssociatedAddress)
    }

    private fun account(address: String, mint: String, solAmount: BigDecimal): SolanaTokenAccountInfo {
        return SolanaTokenAccountInfo(
            value = NewSolanaTokenAccountInfo.Value(),
            address = address,
            mint = mint,
            solAmount = solAmount,
        )
    }

    private fun associatedTokenAddress(
        mint: String,
        programId: SolanaTokenProgram.ID = SolanaTokenProgram.ID.TOKEN,
    ): String {
        return createAssociatedSolanaTokenAddress(
            account = PublicKey(owner),
            mint = PublicKey(mint),
            tokenProgramId = programId,
        ).getOrThrow().toBase58()
    }
}