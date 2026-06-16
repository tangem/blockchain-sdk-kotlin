package com.tangem.blockchain.yieldsupply.providers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthGetStorageAtData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderType
import com.tangem.blockchain.yieldsupply.addressfactory.YieldSupplyContractAddressFactory
import com.tangem.blockchain.yieldsupply.addressfactory.YieldSupplyContractAddresses
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class EthereumYieldSupplyProviderTest {

    private val blockchainDataStorage = mockk<BlockchainDataStorage>(relaxed = true)
    private val dataStorage = AdvancedDataStorage(blockchainDataStorage)
    private val networkProvider = mockk<MultiNetworkProvider<EthereumJsonRpcProvider>>(relaxed = true)
    private val contractAddressFactory = mockk<YieldSupplyContractAddressFactory>(relaxed = true)
    private val wallet = mockk<Wallet>(relaxed = true) {
        every { blockchain } returns Blockchain.Polygon
        every { address } returns USER_ADDRESS
    }

    private val provider = EthereumYieldSupplyProvider(
        multiJsonRpcProvider = networkProvider,
        wallet = wallet,
        contractAddressFactory = contractAddressFactory,
        dataStorage = dataStorage,
    )

    @Before
    fun setUp() {
        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = true,
                isYieldModeSwapEnabled = true,
            ),
        )
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
    }

    // --- checkModuleVersionStatus ---

    @Test
    fun `checkModuleVersionStatus returns NotDeployed when latestImplementationAddress is null`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = null)

        val status = provider.checkModuleVersionStatus()

        assertThat(status).isEqualTo(YieldModuleVersionStatus.NotDeployed)
        // No RPC call should have been attempted
        coVerify(exactly = 0) {
            networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
        }
    }

    @Test
    fun `checkModuleVersionStatus returns NotDeployed when yield module address is ZERO`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery { blockchainDataStorage.getOrNull(any()) } returns null
        // Module factory call returns 32-byte zero word → yieldModuleAddress resolves to ZERO_ADDRESS
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        } returns Result.Success(stringResponse(ZERO_PADDED_32))

        val status = provider.checkModuleVersionStatus()

        assertThat(status).isEqualTo(YieldModuleVersionStatus.NotDeployed)
        coVerify(exactly = 0) {
            networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
        }
    }

    @Test
    fun `checkModuleVersionStatus returns UpToDate when stored implementation matches latest known`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        // dataStorage is consulted twice: once inside getYieldModuleAddress (for cached module addr)
        // and once at step 1 of checkModuleVersionStatus (for cached implementation addr).
        coEvery {
            blockchainDataStorage.getOrNull(any())
        } returns yieldSupplyModuleJson(yieldContractAddress = YIELD_MODULE, implementationAddress = LATEST_IMPL)

        val status = provider.checkModuleVersionStatus()

        assertThat(status).isEqualTo(YieldModuleVersionStatus.UpToDate)
        // Short-circuit — no storage slot read
        coVerify(exactly = 0) {
            networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
        }
    }

    @Test
    fun `checkModuleVersionStatus returns UpgradeAvailable when factory exposes the latest implementation`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery {
            blockchainDataStorage.getOrNull(any())
        } returns yieldSupplyModuleJson(yieldContractAddress = YIELD_MODULE, implementationAddress = null)
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
        } returns Result.Success(stringResponse(OUTDATED_IMPL_RAW))
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        } returns Result.Success(stringResponse(LATEST_IMPL_RAW))

        val status = provider.checkModuleVersionStatus()

        assertThat(status).isEqualTo(
            YieldModuleVersionStatus.UpgradeAvailable(
                currentImplementation = OUTDATED_IMPL,
                latestImplementation = LATEST_IMPL,
            ),
        )
    }

    @Test
    fun `checkModuleVersionStatus returns UpgradeUnavailable when factory implementation differs from latest`() =
        runTest {
            every {
                contractAddressFactory.getYieldSupplyContractAddresses()
            } returns addresses(latestImplementationAddress = LATEST_IMPL)
            coEvery {
                blockchainDataStorage.getOrNull(any())
            } returns yieldSupplyModuleJson(yieldContractAddress = YIELD_MODULE, implementationAddress = null)
            coEvery {
                networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
            } returns Result.Success(stringResponse(OUTDATED_IMPL_RAW))
            // factory returns a third address — neither current nor latest-known
            coEvery {
                networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
            } returns Result.Success(stringResponse(UNKNOWN_IMPL_RAW))

            val status = provider.checkModuleVersionStatus()

            assertThat(status).isEqualTo(
                YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OUTDATED_IMPL),
            )
        }

    @Test
    fun `checkModuleVersionStatus returns UpgradeUnavailable when factory call fails`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery {
            blockchainDataStorage.getOrNull(any())
        } returns yieldSupplyModuleJson(yieldContractAddress = YIELD_MODULE, implementationAddress = null)
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
        } returns Result.Success(stringResponse(OUTDATED_IMPL_RAW))
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        } throws RuntimeException("rpc down")

        val status = provider.checkModuleVersionStatus()

        assertThat(status).isEqualTo(
            YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OUTDATED_IMPL),
        )
    }

    @Test
    fun `checkModuleVersionStatus returns Indeterminate when eth_getStorageAt fails for a deployed module`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery {
            blockchainDataStorage.getOrNull(any())
        } returns yieldSupplyModuleJson(yieldContractAddress = YIELD_MODULE, implementationAddress = null)
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::getStorageAt, any<EthGetStorageAtData>())
        } throws RuntimeException("rpc down")

        val status = provider.checkModuleVersionStatus()

        // Type alone is not enough — downstream `wrapWithUpgradeIfNeeded` throws using `reason`,
        // and the UI surfaces it; if reason is empty or wrong, the user gets a useless error.
        assertThat(status).isInstanceOf(YieldModuleVersionStatus.Indeterminate::class.java)
        val indeterminate = status as YieldModuleVersionStatus.Indeterminate
        assertThat(indeterminate.reason).contains("implementation slot")
        assertThat(indeterminate.reason).contains("rpc down")
    }

    // --- isSwapSpenderAllowed / isSwapTargetAllowed (strict ABI bool decoding) ---

    @Test
    fun `isSwapSpenderAllowed returns true only for ABI true (0x000…001)`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        } returns Result.Success(
            stringResponse("0x0000000000000000000000000000000000000000000000000000000000000001"),
        )

        val allowed = provider.isSwapSpenderAllowed("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")

        assertThat(allowed).isTrue()
    }

    @Test
    fun `isSwapSpenderAllowed returns false for ABI false (0x000…000)`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        } returns Result.Success(
            stringResponse("0x0000000000000000000000000000000000000000000000000000000000000000"),
        )

        val allowed = provider.isSwapSpenderAllowed("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")

        assertThat(allowed).isFalse()
    }

    @Test
    fun `isSwapSpenderAllowed returns false for non-bool value ending in 1 (regression for endsWith bug)`() = runTest {
        // 0x…11 is decimal 17 — old endsWith("1") incorrectly accepted it as true. Strict decoder rejects.
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns addresses(latestImplementationAddress = LATEST_IMPL)
        coEvery {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        } returns Result.Success(
            stringResponse("0x0000000000000000000000000000000000000000000000000000000000000011"),
        )

        val allowed = provider.isSwapSpenderAllowed("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")

        assertThat(allowed).isFalse()
    }

    @Test
    fun `isSwapTargetAllowed returns false when swapExecutionRegistryAddress is null`() = runTest {
        every {
            contractAddressFactory.getYieldSupplyContractAddresses()
        } returns YieldSupplyContractAddresses(
            factoryContractAddress = FACTORY,
            processorContractAddress = PROCESSOR,
            swapExecutionRegistryAddress = null,
            latestImplementationAddress = LATEST_IMPL,
            providerType = YieldSupplyProviderType.AaveV3,
        )

        val allowed = provider.isSwapTargetAllowed("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")

        assertThat(allowed).isFalse()
        coVerify(exactly = 0) {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        }
    }

    @Test
    fun `isSwapSpenderAllowed returns false when toggle is off`() = runTest {
        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = true,
                isYieldModeSwapEnabled = false,
            ),
        )

        val allowed = provider.isSwapSpenderAllowed("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")

        assertThat(allowed).isFalse()
        coVerify(exactly = 0) {
            networkProvider.performRequest(EthereumJsonRpcProvider::call, any<EthCallObject>())
        }
    }

    // --- helpers ---

    private fun addresses(latestImplementationAddress: String?) = YieldSupplyContractAddresses(
        factoryContractAddress = FACTORY,
        processorContractAddress = PROCESSOR,
        swapExecutionRegistryAddress = REGISTRY,
        latestImplementationAddress = latestImplementationAddress,
        providerType = YieldSupplyProviderType.AaveV3,
    )

    private fun yieldSupplyModuleJson(yieldContractAddress: String?, implementationAddress: String?): String {
        val parts = mutableListOf<String>()
        if (yieldContractAddress != null) parts += "\"contractAddress\":\"$yieldContractAddress\""
        if (implementationAddress != null) parts += "\"implementationAddress\":\"$implementationAddress\""
        return "{${parts.joinToString(",")}}"
    }

    private fun stringResponse(value: String): JsonRPCResponse = JsonRPCResponse(
        id = "1",
        jsonRpc = "2.0",
        result = value,
        error = null,
    )

    private companion object {
        const val USER_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val YIELD_MODULE = "0x2222222222222222222222222222222222222222"
        const val FACTORY = "0x3333333333333333333333333333333333333333"
        const val PROCESSOR = "0x4444444444444444444444444444444444444444"
        const val REGISTRY = "0x5555555555555555555555555555555555555555"

        const val LATEST_IMPL = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val OUTDATED_IMPL = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

        // 32-byte ABI-encoded address responses (0x + 24 leading zero hex chars + 40-char address)
        const val LATEST_IMPL_RAW =
            "0x000000000000000000000000aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val OUTDATED_IMPL_RAW =
            "0x000000000000000000000000bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        const val UNKNOWN_IMPL_RAW =
            "0x000000000000000000000000cccccccccccccccccccccccccccccccccccccccc"
        const val ZERO_PADDED_32 =
            "0x0000000000000000000000000000000000000000000000000000000000000000"
    }
}