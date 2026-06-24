package com.tangem.blockchain.yieldsupply.providers

import android.util.Log
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthGetStorageAtData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.AllowanceERC20TokenCallData
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainSavedData.YieldSupplyModule
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.orZero
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.moshi
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderType
import com.tangem.blockchain.yieldsupply.addressfactory.YieldSupplyContractAddressFactory
import com.tangem.blockchain.yieldsupply.addressfactory.YieldSupplyContractAddresses
import com.tangem.blockchain.yieldsupply.providers.ethereum.converters.EthereumYieldSupplyStatusConverter
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyContractAddressCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyModuleCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.processor.EthereumYieldSupplyServiceFeeCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.registry.EthereumYieldSupplyAllowedAddressCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyBalanceCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEffectiveProtocolBalanceCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyStatusCallData
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

@OptIn(ExperimentalStdlibApi::class)
internal class EthereumYieldSupplyProvider(
    private val multiJsonRpcProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    private val wallet: Wallet,
    private val contractAddressFactory: YieldSupplyContractAddressFactory,
    private val dataStorage: AdvancedDataStorage,
) : YieldSupplyProvider {

    private val stringAdapter by lazy { moshi.adapter<String>() }
    private val ethereumYieldSupplyStatusConverter by lazy {
        EthereumYieldSupplyStatusConverter(wallet.blockchain.decimals())
    }

    private val addressService = EthereumAddressService()

    override fun isSupported(): Boolean = contractAddressFactory.isSupported()

    override fun getYieldSupplyContractAddresses(): YieldSupplyContractAddresses {
        return contractAddressFactory.getYieldSupplyContractAddresses()
    }

    override suspend fun getServiceFee(): BigDecimal {
        return multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldSupplyContractAddresses().processorContractAddress,
                data = EthereumYieldSupplyServiceFeeCallData.dataHex,
            ),
        ).extractResult().hexToBigDecimal().movePointLeft(BASIS_POINTS_DECIMALS)
    }

    override suspend fun getYieldModuleAddress(): String = try {
        val supplyContractAddresses = getYieldSupplyContractAddresses()
        val stored = dataStorage.getOrNull<YieldSupplyModule>(
            key = storeKey(supplyContractAddresses.providerType),
        )
        val storedYieldModuleAddress = stored?.yieldContractAddress ?: EthereumUtils.ZERO_ADDRESS

        val hasMatchingCachedFactory = stored?.factoryAddress
            ?.equals(supplyContractAddresses.factoryContractAddress, ignoreCase = true) == true

        if (storedYieldModuleAddress == EthereumUtils.ZERO_ADDRESS ||
            !addressService.validate(storedYieldModuleAddress) ||
            !hasMatchingCachedFactory
        ) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = supplyContractAddresses.factoryContractAddress,
                    data = EthereumYieldSupplyModuleCallData(wallet.address).dataHex,
                ),
            ).extractResult()
                .takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

            val yieldModuleAddress = HEX_PREFIX + rawContractAddress

            if (!addressService.validate(yieldModuleAddress)) {
                EthereumUtils.ZERO_ADDRESS
            } else {
                dataStorage.store(
                    key = storeKey(supplyContractAddresses.providerType),
                    value = YieldSupplyModule(
                        yieldContractAddress = yieldModuleAddress,
                        factoryAddress = supplyContractAddresses.factoryContractAddress,
                    ),
                )

                yieldModuleAddress
            }
        } else {
            storedYieldModuleAddress
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to get yield module address: ${e.message}")
        EthereumUtils.ZERO_ADDRESS
    }

    override suspend fun calculateYieldModuleAddress(): String {
        val rawYieldModuleAddress = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldSupplyContractAddresses().factoryContractAddress,
                data = EthereumYieldSupplyContractAddressCallData(wallet.address).dataHex,
            ),
        ).extractResult()

        val yieldModuleAddress = HEX_PREFIX + rawYieldModuleAddress.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

        return yieldModuleAddress
    }

    override suspend fun getYieldSupplyStatus(tokenContractAddress: String): YieldSupplyStatus? = try {
        val result = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldModuleAddress(),
                data = EthereumYieldSupplyStatusCallData(tokenContractAddress).dataHex,
            ),
        ).extractResult()
        ethereumYieldSupplyStatusConverter.convert(result)
    } catch (exception: Exception) {
        Log.w(this::class.java.simpleName, "Failed to get yield supply status", exception)
        null
    }

    override suspend fun getBalance(yieldSupplyStatus: YieldSupplyStatus, token: Token): Amount = coroutineScope {
        val balanceDeferred = async {
            val rawBalance = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = getYieldModuleAddress(),
                    data = EthereumYieldSupplyBalanceCallData(token.contractAddress).dataHex,
                ),
            )

            requireNotNull(
                EthereumUtils.parseEthereumDecimal(
                    rawBalance.extractResult(),
                    token.decimals,
                ),
            ) { "Failed to parse token balance. Token: ${token.name}. Balance: ${rawBalance.extractResult()}" }
        }

        val isAllowedToSpendDeferred = async {
            isAllowedToSpend(token)
        }

        val effectiveProtocolBalance = async {
            if (yieldSupplyStatus.isActive) {
                getEffectiveProtocolBalance(token)
            } else {
                null
            }
        }

        Amount(
            value = balanceDeferred.await(),
            blockchain = wallet.blockchain,
            currencySymbol = token.symbol,
            type = AmountType.TokenYieldSupply(
                token = token,
                isActive = yieldSupplyStatus.isActive,
                isInitialized = yieldSupplyStatus.isInitialized,
                isAllowedToSpend = isAllowedToSpendDeferred.await(),
                effectiveProtocolBalance = effectiveProtocolBalance.await(),
            ),
        )
    }

    override suspend fun getEffectiveProtocolBalance(token: Token): BigDecimal {
        val rawProtocolBalance = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldModuleAddress(),
                data = EthereumYieldSupplyEffectiveProtocolBalanceCallData(
                    tokenContractAddress = token.contractAddress,
                ).dataHex,
            ),
        ).extractResult()

        return EthereumUtils.parseEthereumDecimal(
            rawProtocolBalance,
            token.decimals,
        ).orZero()
    }

    override suspend fun isAllowedToSpend(token: Token): Boolean {
        val allowanceRaw = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = token.contractAddress,
                data = AllowanceERC20TokenCallData(
                    ownerAddress = wallet.address,
                    spenderAddress = getYieldModuleAddress(),
                ).dataHex,
            ),
        ).extractResult()

        val allowanceValue = EthereumUtils.parseEthereumDecimal(
            value = allowanceRaw,
            decimalsCount = wallet.blockchain.decimals(),
        ).orZero()

        return allowanceValue >= (Int.MAX_VALUE / 2).toBigDecimal()
    }

    @Suppress("ReturnCount")
    override suspend fun checkModuleVersionStatus(): YieldModuleVersionStatus {
        val contractAddresses = getYieldSupplyContractAddresses()
        val latestKnown = contractAddresses.latestImplementationAddress
            ?: return YieldModuleVersionStatus.NotDeployed

        val stored = dataStorage.getOrNull<YieldSupplyModule>(
            key = storeKey(contractAddresses.providerType),
        )

        val yieldModuleAddress = getYieldModuleAddress()
        if (yieldModuleAddress == EthereumUtils.ZERO_ADDRESS) {
            val hasPriorDeployRecorded = stored?.yieldContractAddress != null &&
                !stored.yieldContractAddress.equals(EthereumUtils.ZERO_ADDRESS, ignoreCase = true)
            return if (hasPriorDeployRecorded) {
                YieldModuleVersionStatus.Indeterminate(
                    reason = "yield module address resolved to ZERO despite valid cached deployment",
                )
            } else {
                YieldModuleVersionStatus.NotDeployed
            }
        }

        // Step 1: Check cached implementation address in storage
        val storedImpl = stored?.implementationAddress
        if (storedImpl != null && storedImpl.equals(latestKnown, ignoreCase = true)) {
            return YieldModuleVersionStatus.UpToDate
        }

        // Step 2: Read current implementation from proxy storage slot via eth_getStorageAt
        val currentImpl = try {
            val result = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::getStorageAt,
                data = EthGetStorageAtData(
                    address = yieldModuleAddress,
                    position = IMPLEMENTATION_SLOT,
                ),
            ).extractResult()
            HEX_PREFIX + result.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get module implementation: ${e.message}")
            // Module is provably deployed (we just resolved yieldModuleAddress != ZERO), but we
            // cannot read its implementation slot — version is unknown, refuse to proceed.
            return YieldModuleVersionStatus.Indeterminate(
                reason = "implementation slot read failed: ${e.message}",
            )
        }

        // Step 3: Save to storage only if implementation address changed
        saveImplementationIfChanged(contractAddresses.providerType, stored, currentImpl)

        // Step 4: Compare with latest known
        if (currentImpl.equals(latestKnown, ignoreCase = true)) {
            return YieldModuleVersionStatus.UpToDate
        }

        // Step 5: Module is outdated — check if the module's OWN factory authorizes the upgrade
        return resolveOutdatedYieldModuleStatus(
            multiJsonRpcProvider = multiJsonRpcProvider,
            contractAddresses = contractAddresses,
            yieldModuleAddress = yieldModuleAddress,
            currentImpl = currentImpl,
            latestKnown = latestKnown,
        )
    }

    override suspend fun isSwapSpenderAllowed(spenderAddress: String): Boolean {
        if (!DepsContainer.blockchainFeatureToggles.isYieldModeSwapEnabled) return false
        return isRegistryAddressAllowed(EthereumYieldSupplyAllowedAddressCallData.allowedSpenders(spenderAddress))
    }

    override suspend fun isSwapTargetAllowed(targetAddress: String): Boolean {
        if (!DepsContainer.blockchainFeatureToggles.isYieldModeSwapEnabled) return false
        return isRegistryAddressAllowed(EthereumYieldSupplyAllowedAddressCallData.allowedTargets(targetAddress))
    }

    private suspend fun isRegistryAddressAllowed(callData: EthereumYieldSupplyAllowedAddressCallData): Boolean = try {
        val registryAddress = getYieldSupplyContractAddresses().swapExecutionRegistryAddress
        if (registryAddress == null) {
            false
        } else {
            val result = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = registryAddress,
                    data = callData.dataHex,
                ),
            ).extractResult()
            result.removePrefix(HEX_PREFIX).trimStart('0') == "1"
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to check registry address: ${e.message}")
        false
    }

    private suspend fun saveImplementationIfChanged(
        providerType: YieldSupplyProviderType,
        stored: YieldSupplyModule?,
        currentImpl: String,
    ) {
        if (stored?.implementationAddress?.equals(currentImpl, ignoreCase = true) == true) return
        val current = dataStorage.getOrNull<YieldSupplyModule>(key = storeKey(providerType))
        dataStorage.store(
            key = storeKey(providerType),
            value = current?.copy(implementationAddress = currentImpl)
                ?: YieldSupplyModule(implementationAddress = currentImpl),
        )
    }

    private fun storeKey(providerType: YieldSupplyProviderType) = "yield-supply-${providerType.key}" +
        "-${wallet.publicKey.blockchainKey.toCompressedPublicKey().toHexString()}" +
        "-${wallet.address}-${wallet.blockchain.id}"

    private fun Result<JsonRPCResponse>.extractResult(): String = extractYieldRpcResult(stringAdapter)

    private companion object {
        const val BASIS_POINTS_DECIMALS = 4
        const val TAG = "EthereumYieldSupplyProvider"

        /** EIP-1967 implementation storage slot */
        const val IMPLEMENTATION_SLOT =
            "0x360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc"
    }
}