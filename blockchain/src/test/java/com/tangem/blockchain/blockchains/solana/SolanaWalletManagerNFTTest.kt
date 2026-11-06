package com.tangem.blockchain.blockchains.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.nft.MplCoreAccountBytes
import com.tangem.blockchain.blockchains.solana.nft.MplCoreProgram
import com.tangem.blockchain.blockchains.solana.nft.SolanaNFTFixtures
import com.tangem.blockchain.blockchains.solana.nft.SolanaNFTTransferExtras
import com.tangem.blockchain.blockchains.solana.nft.TokenMetadataProgram
import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.model.FeeInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.NewSolanaAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaBlockhashInfo
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcApi
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.p2p.solanaj.core.PublicKey
import java.math.BigDecimal
import java.util.Base64

internal class SolanaWalletManagerNFTTest {

    private val wallet = PublicKey("CUfZZHt9nNsSnQUv5jhGGLbRRtudpWvzoSwbJQf7T9PW")
    private val recipient = PublicKey("A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo")
    private val coreAsset = PublicKey("3xYi4gjZ8HKp8N2yEHiYKjVrTKX3K9KcC33qYxWzbDFZ")
    private val pnftMint = PublicKey("7Kny3iBH1MSjeeFQMpGc2sWsFscyuoosSBHMS67ZoPX4")

    private val api: SolanaRpcApi = mockk()
    private val rpcClient: SolanaRpcClient = mockk {
        every { baseUrl } returns "https://solana.test/"
        every { endpoint } returns "https://solana.test/"
        every { api } returns this@SolanaWalletManagerNFTTest.api
    }

    private var previousDeps: Pair<BlockchainSdkConfig, BlockchainFeatureToggles>? = null

    @Before
    fun setUp() {
        previousDeps = runCatching {
            DepsContainer.blockchainSdkConfig to DepsContainer.blockchainFeatureToggles
        }.getOrNull()

        every { api.getAccountInfoNew(any(), any()) } returns accountInfo(null)
        every { api.getLatestBlockhashInfo(any()) } returns SolanaBlockhashInfo(blockhash = recipient.toBase58(), 1)
        every { api.getFeeForMessage(any<SolanaTransaction>(), any()) } returns mockk<FeeInfo> {
            every { value } returns NETWORK_FEE
        }
        every { api.getMinimumBalanceForRentExemption(TOKEN_ACCOUNT_SPACE) } returns TOKEN_ACCOUNT_RENT
        every { api.getMinimumBalanceForRentExemption(TOKEN_RECORD_SPACE) } returns TOKEN_RECORD_RENT
    }

    @After
    fun tearDown() {
        val (config, toggles) = previousDeps
            ?: mockk<BlockchainSdkConfig>(relaxed = true) to BlockchainFeatureToggles(isYieldSupplyEnabled = false)
        DepsContainer.onInit(config = config, featureToggles = toggles)
    }

    @Test
    fun `GIVEN toggle off and pnft to a fresh recipient WHEN getFee THEN asset is still routed by the chain`() =
        runTest {
            // Arrange
            setToggle(enabled = false)
            givenProgrammableNft()

            // Act
            val fee = manager().getFee(nftTransaction(pnftMint))

            // Assert
            assertThat(fee.lamports()).isEqualTo(NETWORK_FEE + TOKEN_ACCOUNT_RENT + TOKEN_RECORD_RENT)
        }

    @Test
    fun `GIVEN core asset WHEN getFee THEN fee is the network fee`() = runTest {
        // Arrange
        givenAccount(coreAsset, MplCoreProgram.programId, MplCoreAccountBytes.asset(wallet, null))

        // Act
        val fee = manager().getFee(nftTransaction(coreAsset))

        // Assert
        assertThat(fee.lamports()).isEqualTo(NETWORK_FEE)
        verify(exactly = 0) { api.getMinimumBalanceForRentExemption(any()) }
    }

    @Test
    fun `GIVEN pnft to a fresh recipient WHEN getFee THEN fee includes token account and record rent`() = runTest {
        // Arrange
        givenProgrammableNft()

        // Act
        val fee = manager().getFee(nftTransaction(pnftMint))

        // Assert
        assertThat(fee.lamports()).isEqualTo(NETWORK_FEE + TOKEN_ACCOUNT_RENT + TOKEN_RECORD_RENT)
    }

    @Test
    fun `GIVEN recipient token account without record WHEN getFee THEN fee includes only record rent`() = runTest {
        // Arrange
        givenProgrammableNft()
        givenAccount(ata(recipient), SolanaTokenProgram.ID.TOKEN.value, ByteArray(TOKEN_ACCOUNT_SPACE.toInt()))

        // Act
        val fee = manager().getFee(nftTransaction(pnftMint))

        // Assert
        assertThat(fee.lamports()).isEqualTo(NETWORK_FEE + TOKEN_RECORD_RENT)
        verify(exactly = 0) { api.getMinimumBalanceForRentExemption(TOKEN_ACCOUNT_SPACE) }
    }

    private fun givenProgrammableNft() {
        givenAccount(pnftMint, SolanaTokenProgram.ID.TOKEN.value, ByteArray(MINT_SPACE))
        givenAccount(
            TokenMetadataProgram.metadata(pnftMint),
            TokenMetadataProgram.programId,
            SolanaNFTFixtures.pnftMetadataAccount,
        )
        givenAccount(
            TokenMetadataProgram.tokenRecord(pnftMint, ata(wallet)),
            TokenMetadataProgram.programId,
            UNLOCKED_TOKEN_RECORD,
        )
    }

    private fun givenAccount(address: PublicKey, owner: PublicKey, data: ByteArray) {
        val value = mockk<NewSolanaAccountInfo.Value> {
            every { this@mockk.owner } returns owner.toBase58()
            every { this@mockk.data } returns listOf(Base64.getEncoder().encodeToString(data), "base64")
        }
        every { api.getAccountInfoNew(address, any()) } returns accountInfo(value)
    }

    private fun accountInfo(value: NewSolanaAccountInfo.Value?) = mockk<NewSolanaAccountInfo> {
        every { this@mockk.value } returns value
    }

    private fun setToggle(enabled: Boolean) {
        DepsContainer.onInit(
            config = mockk(relaxed = true),
            featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false, isSolanaCoreNftEnabled = enabled),
        )
    }

    private fun manager() = SolanaWalletManager(
        wallet = Wallet(
            blockchain = Blockchain.Solana,
            addresses = setOf(Address(wallet.toBase58(), AddressType.Default)),
            publicKey = Wallet.PublicKey(seedKey = wallet.toByteArray(), derivationType = null),
            tokens = emptySet(),
        ),
        providers = listOf(rpcClient),
    )

    private fun nftTransaction(asset: PublicKey, extras: TransactionExtras = SolanaNFTTransferExtras) =
        TransactionData.Uncompiled(
            amount = Amount(
                token = Token(name = "NFT", symbol = "NFT", contractAddress = asset.toBase58(), decimals = 0),
                value = BigDecimal.ONE,
            ),
            fee = null,
            sourceAddress = wallet.toBase58(),
            destinationAddress = recipient.toBase58(),
            extras = extras,
        )

    private fun ata(owner: PublicKey): PublicKey =
        createAssociatedSolanaTokenAddress(owner, pnftMint, SolanaTokenProgram.ID.TOKEN).getOrThrow()

    private fun Result<TransactionFee>.lamports(): Long {
        val fee = (this as Result.Success).data as TransactionFee.Single
        return fee.normal.amount.value!!
            .movePointRight(SOL_DECIMALS)
            .longValueExact()
    }

    private companion object {
        const val NETWORK_FEE = 5_000L
        const val TOKEN_ACCOUNT_SPACE = 165L
        const val TOKEN_RECORD_SPACE = 80L
        const val TOKEN_ACCOUNT_RENT = 2_039_280L
        const val TOKEN_RECORD_RENT = 1_447_680L
        const val MINT_SPACE = 82
        const val SOL_DECIMALS = 9
        val UNLOCKED_TOKEN_RECORD = byteArrayOf(11, -1, 0)
    }
}