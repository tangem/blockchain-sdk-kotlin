package com.tangem.blockchain.collectibles.providers.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.solanaj.model.NewSolanaAccountInfo
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.p2p.solanaj.core.PublicKey

/**
 * The expected addresses were derived by a separate implementation of `findProgramAddress`, not by the code under
 * test, and that implementation was checked against two values known from elsewhere: the config address in
 * [GachaVaultProgram] and the live devnet vault of [COLD_OWNER], `aBRHcE97NY1MjVqGbc2LX2cApfPg9r5h5T1KNZS93oA`.
 *
 * The network mock answers for the config address only. Reading any other account is left unstubbed on purpose, so
 * a derivation that drifts to the wrong account fails the test instead of being served the config anyway.
 */
internal class SolanaCollectiblesVaultProviderTest {

    @Test
    fun `GIVEN a config account WHEN reading the usdc account THEN its mint and the vault ATA are returned`() =
        runTest {
            // Arrange
            val provider = createProvider(configAccount())

            // Act
            val result = provider.getCollectiblesVaultUsdc()

            // Assert
            val account = (result as Result.Success).data
            assertThat(account.mint).isEqualTo(USDC_MINT)
            assertThat(account.address).isEqualTo(VAULT_USDC)
        }

    @Test
    fun `GIVEN no config account WHEN reading the usdc account THEN the miss is not reported as a missing account`() =
        runTest {
            // Arrange
            // A missing vault means "not onboarded"; a missing config means the program is not where we think it is.
            // Letting AccountNotFound through would let a caller mistake the second for the first.
            val provider = createProvider(absentAccount())

            // Act
            val result = provider.getCollectiblesVaultUsdc()

            // Assert
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(BlockchainSdkError.CustomError::class.java)
            assertThat(error).isNotInstanceOf(BlockchainSdkError.AccountNotFound::class.java)
        }

    private fun createProvider(networkService: SolanaNetworkService) = SolanaCollectiblesVaultProvider(
        coldOwner = PublicKey(COLD_OWNER),
        multiNetworkProvider = MultiNetworkProvider(
            providers = listOf(networkService),
            blockchain = Blockchain.SolanaTestnet,
        ),
    )

    private fun configAccount(): SolanaNetworkService = networkService(
        Result.Success(accountValue(owner = GachaVaultProgram.programId.toBase58(), data = CONFIG_DATA)),
    )

    private fun absentAccount(): SolanaNetworkService =
        networkService(Result.Failure(BlockchainSdkError.AccountNotFound()))

    private fun networkService(answer: Result<NewSolanaAccountInfo.Value>): SolanaNetworkService = mockk {
        every { baseUrl } returns "https://provider.test/rpc"
        coEvery { getAccountInfoIfExist(match { it.toBase58() == CONFIG_ADDRESS }) } returns answer
    }

    private fun accountValue(owner: String, data: String): NewSolanaAccountInfo.Value = NewSolanaAccountInfo.Value(
        hashMapOf<String, Any>(
            "data" to listOf(data, "base64"),
            "executable" to false,
            "lamports" to 1L,
            "owner" to owner,
            "space" to 0L,
        ),
    )

    private companion object {
        const val COLD_OWNER = "CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31"
        const val CONFIG_ADDRESS = "53EDbBLB1sQuVv6aH7us9TQ7XSGXFijPmPdAGNcY8Y1d"
        const val USDC_MINT = "Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr"
        const val VAULT_USDC = "6atEYZQxh5sYXjnwS5WfVvnh6p8wUXibc2xcD2Gftfqs"

        /** The real devnet `Config` account, the same bytes [GachaConfigParserTest] parses. */
        const val CONFIG_DATA = "mwyq4B76zIKtbdnm3fQfKiZwulw19xJe+Y62TcL9MF5YyYuupu7XZOkoOVUJZf/U1krKr0bUXfcxjltP" +
            "V8kMSH1gYl2Cm4N7hqQIUEd4g/rZZ93XJGmfY1Im7dYaVhpfB0GPFwNaf39/LRy7KpYTz3xzEH7Z5ZuyXLdQy82fbyK8F4zf" +
            "pFdYmoNiez2h0gaidjS66O7bgD1GanQ/NMIfiiDM0v1mCX+YZAAAAQH/"
    }
}