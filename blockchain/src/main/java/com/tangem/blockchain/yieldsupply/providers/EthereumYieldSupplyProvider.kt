package com.tangem.blockchain.yieldsupply.providers

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.AllowanceERC20TokenCallData
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainSavedData.YieldSupplyModule
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
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
        val storedYieldModuleAddress = dataStorage.getOrNull<YieldSupplyModule>(
            key = storeKey(supplyContractAddresses.providerType),
        )?.yieldContractAddress ?: EthereumUtils.ZERO_ADDRESS

        if (storedYieldModuleAddress == EthereumUtils.ZERO_ADDRESS ||
            !addressService.validate(storedYieldModuleAddress)
        ) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = supplyContractAddresses.factoryContractAddress,
                    data = EthereumYieldSupplyModuleCallData(wallet.address).dataHex,
                ),
            ).extractResult()
                .takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

            if (!addressService.validate(rawContractAddress)) {
                EthereumUtils.ZERO_ADDRESS
            } else {
                val yieldModuleAddress = HEX_PREFIX + rawContractAddress

                dataStorage.store(
                    key = storeKey(supplyContractAddresses.providerType),
                    value = YieldSupplyModule(yieldModuleAddress),
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

    private fun storeKey(providerType: YieldSupplyProviderType) = "yield-supply-${providerType.key}" +
        "-${wallet.publicKey.blockchainKey.toCompressedPublicKey().toHexString()}" +
        "-${wallet.address}-${wallet.blockchain.id}"

    private fun Result<JsonRPCResponse>.extractResult(): String = extractResult(adapter = stringAdapter)

    private fun <Body> Result<JsonRPCResponse>.extractResult(adapter: JsonAdapter<Body>): Body {
        return when (this) {
            is Result.Success -> {
                runCatching { adapter.fromJsonValue(data.result) }.getOrNull()
                    ?: throw data.error?.let { error ->
                        BlockchainSdkError.Ethereum.getApiErrorByCode(code = error.code, message = error.message)
                    } ?: BlockchainSdkError.CustomError("Unknown response format")
            }
            is Result.Failure -> {
                throw error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
            }
        }
    }

    private companion object {
        const val BASIS_POINTS_DECIMALS = 4
        const val TAG = "EthereumYieldSupplyProvider"
    }
}