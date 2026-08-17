package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.hedera.models.HederaTokenType
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class HederaWalletManagerErc20FeeErrorTest {

    @Before
    fun setup() {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = false,
                isHederaErc20Enabled = true,
            ),
        )
    }

    @Test
    fun getFee_propagatesAddressResolutionError_insteadOfFailedToLoadFee() = runTest {
        val contractAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val token = Token(name = "Token", symbol = "TOK", contractAddress = contractAddress, decimals = 8)
        val wallet = Wallet(
            blockchain = Blockchain.Hedera,
            addresses = setOf(Address("0.0.1001")),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(32) { 1 }, derivationType = null),
            tokens = setOf(token),
        )
        val dataStorage = AdvancedDataStorage(InMemoryBlockchainDataStorage())
        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = wallet.address,
                associatedTokens = emptySet(),
                tokenTypes = mapOf(contractAddress to HederaTokenType.ERC20.name),
                tokenEvmAddresses = mapOf(contractAddress to contractAddress),
                isCacheCleared = true,
            ),
        )

        val networkService = mockk<HederaNetworkService>()
        coEvery { networkService.getAccountEvmAddress(any()) } returns
            Result.Failure(BlockchainSdkError.Hedera.EvmAddressMismatchBetweenNodes)

        val manager = HederaWalletManager(
            wallet = wallet,
            transactionBuilder = HederaTransactionBuilder(curve = EllipticCurve.Ed25519, wallet = wallet),
            networkService = networkService,
            dataStorage = dataStorage,
            accountCreator = mockk(),
        )

        val result = manager.getFee(Amount(token, BigDecimal.ONE), "0.0.2002")

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressMismatchBetweenNodes)
    }

    private class InMemoryBlockchainDataStorage : BlockchainDataStorage {
        private val data = linkedMapOf<String, String>()

        override suspend fun getOrNull(key: String): String? = data[key]

        override suspend fun store(key: String, value: String) {
            data[key] = value
        }

        override suspend fun remove(key: String) {
            data.remove(key)
        }
    }
}