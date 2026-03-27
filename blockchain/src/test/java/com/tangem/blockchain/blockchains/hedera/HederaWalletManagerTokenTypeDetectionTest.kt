package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.hedera.models.HederaTokenType
import com.tangem.blockchain.blockchains.hedera.network.HederaContractResponse
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HederaWalletManagerTokenTypeDetectionTest {

    @Test
    fun requirementsCondition_detectsAndCachesTypeOnCacheMiss() = runTest {
        val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val token = Token(name = "Token", symbol = "TOK", contractAddress = tokenContractAddress, decimals = 8)
        val wallet = testWallet(token = token)
        val storage = InMemoryBlockchainDataStorage()
        val dataStorage = AdvancedDataStorage(storage)
        val networkService = mockk<HederaNetworkService>()

        coEvery { networkService.getContractInfo(tokenContractAddress) } returns Result.Success(
            HederaContractResponse(
                contractId = "0.0.7777",
                evmAddress = tokenContractAddress,
            ),
        )

        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = wallet.address,
                associatedTokens = emptySet(),
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
        val cached = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)

        assertThat(condition).isNull()
        assertThat(cached?.tokenTypes?.get(tokenContractAddress)).isEqualTo(HederaTokenType.ERC20.name)
        assertThat(cached?.resolvedContractAddresses?.get(tokenContractAddress)).isEqualTo("0.0.7777")
        assertThat(cached?.tokenEvmAddresses?.get(tokenContractAddress)).isEqualTo(tokenContractAddress)
    }

    @Test
    fun requirementsCondition_doesNotFallbackToHts_whenContractInfoFailsForErc20Address() = runTest {
        val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val token = Token(name = "Token", symbol = "TOK", contractAddress = tokenContractAddress, decimals = 8)
        val wallet = testWallet(token = token)
        val storage = InMemoryBlockchainDataStorage()
        val dataStorage = AdvancedDataStorage(storage)
        val networkService = mockk<HederaNetworkService>()

        coEvery { networkService.getContractInfo(tokenContractAddress) } returns
            Result.Failure(BlockchainSdkError.FailedToLoadFee)

        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = wallet.address,
                associatedTokens = emptySet(),
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
        val cached = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)

        assertThat(condition).isNull()
        assertThat(cached?.tokenTypes?.get(tokenContractAddress)).isEqualTo(HederaTokenType.ERC20.name)
        assertThat(cached?.tokenEvmAddresses?.get(tokenContractAddress)).isEqualTo(tokenContractAddress)
    }

    @Test
    fun requirementsCondition_recoversMissingEvmAddressForCachedErc20() = runTest {
        val tokenContractAddress = "0.0.8888"
        val tokenEvmAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val token = Token(name = "Token", symbol = "TOK", contractAddress = tokenContractAddress, decimals = 8)
        val wallet = testWallet(token = token)
        val storage = InMemoryBlockchainDataStorage()
        val dataStorage = AdvancedDataStorage(storage)
        val networkService = mockk<HederaNetworkService>()

        coEvery { networkService.detectTokenType(tokenContractAddress) } returns Result.Success(HederaTokenType.ERC20)
        coEvery { networkService.getContractEvmAddress(tokenContractAddress) } returns Result.Success(tokenEvmAddress)

        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = wallet.address,
                associatedTokens = emptySet(),
                tokenTypes = mapOf(tokenContractAddress to HederaTokenType.ERC20.name),
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
        val cached = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)

        assertThat(condition).isNull()
        assertThat(cached?.tokenTypes?.get(tokenContractAddress)).isEqualTo(HederaTokenType.ERC20.name)
        assertThat(cached?.tokenEvmAddresses?.get(tokenContractAddress)).isEqualTo(tokenEvmAddress)
    }

    @Test
    fun requirementsCondition_keepsCachedErc20TypeWhenDetectionFails() = runTest {
        val tokenContractAddress = "0.0.8888"
        val token = Token(name = "Token", symbol = "TOK", contractAddress = tokenContractAddress, decimals = 8)
        val wallet = testWallet(token = token)
        val storage = InMemoryBlockchainDataStorage()
        val dataStorage = AdvancedDataStorage(storage)
        val networkService = mockk<HederaNetworkService>()

        coEvery { networkService.detectTokenType(tokenContractAddress) } returns
            Result.Failure(BlockchainSdkError.FailedToLoadFee)

        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = wallet.address,
                associatedTokens = emptySet(),
                tokenTypes = mapOf(tokenContractAddress to HederaTokenType.ERC20.name),
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
        val cached = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)

        assertThat(condition).isNull()
        assertThat(cached?.tokenTypes?.get(tokenContractAddress)).isEqualTo(HederaTokenType.ERC20.name)
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