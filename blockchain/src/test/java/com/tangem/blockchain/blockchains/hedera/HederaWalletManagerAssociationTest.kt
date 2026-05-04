package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.hedera.models.HederaTokenType
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.common.*
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

class HederaWalletManagerAssociationTest {

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
    fun requirementsCondition_returnsNull_whenResolvedTokenAlreadyAssociated() = runTest {
        val token = Token(name = "Token", symbol = "TOK", contractAddress = "0xabc123", decimals = 8)
        val wallet = testWallet(token = token)
        val storage = InMemoryBlockchainDataStorage()
        val dataStorage = AdvancedDataStorage(storage)
        val networkService = mockk<HederaNetworkService>()

        coEvery { networkService.baseUrl } returns "https://example.com"
        coEvery { networkService.getUsdExchangeRate() } returns Result.Success("0.1".toBigDecimal())

        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = wallet.address,
                associatedTokens = setOf("0.0.123"),
                tokenTypes = mapOf(token.contractAddress to HederaTokenType.HTS.name),
                resolvedContractAddresses = mapOf(token.contractAddress to "0.0.123"),
                isCacheCleared = true,
            ),
        )

        val manager = HederaWalletManager(
            wallet = wallet,
            transactionBuilder = HederaTransactionBuilder(curve = EllipticCurve.Ed25519, wallet = wallet),
            networkService = networkService,
            dataStorage = dataStorage,
            accountCreator = mockk(),
        )

        val condition = manager.requirementsCondition(CryptoCurrencyType.Token(token))

        assertThat(condition).isNull()
    }

    private fun testWallet(token: Token): Wallet {
        return Wallet(
            blockchain = Blockchain.Hedera,
            addresses = setOf(Address("0.0.1001")),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(32) { 1 }, derivationType = null),
            tokens = setOf(token),
        )
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