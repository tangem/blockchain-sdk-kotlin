package com.tangem.blockchain.yieldsupply.providers

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
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
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyProtocolBalanceCallData
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

    override suspend fun getYieldContract(): String {
        val supplyContractAddresses = getYieldSupplyContractAddresses()
        val storedYieldContractAddress = dataStorage.getOrNull<YieldSupplyModule>(
            key = storeKey(supplyContractAddresses.providerType),
        )?.yieldContractAddress ?: EthereumUtils.ZERO_ADDRESS

        return if (storedYieldContractAddress == EthereumUtils.ZERO_ADDRESS) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = supplyContractAddresses.factoryContractAddress,
                    data = EthereumYieldSupplyModuleCallData(wallet.address).dataHex,
                ),
            ).extractResult()

            val yieldContractAddress = HEX_PREFIX + rawContractAddress.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

            dataStorage.store(
                key = storeKey(supplyContractAddresses.providerType),
                value = YieldSupplyModule(yieldContractAddress),
            )

            yieldContractAddress
        } else {
            storedYieldContractAddress
        }
    }

    override suspend fun calculateYieldContract(): String {
        val supplyContractAddresses = getYieldSupplyContractAddresses()
        val storedYieldContractAddress = dataStorage.getOrNull<YieldSupplyModule>(
            key = storeKey(supplyContractAddresses.providerType),
        )?.yieldContractAddress ?: EthereumUtils.ZERO_ADDRESS

        return if (storedYieldContractAddress == EthereumUtils.ZERO_ADDRESS) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = getYieldSupplyContractAddresses().factoryContractAddress,
                    data = EthereumYieldSupplyContractAddressCallData(wallet.address).dataHex,
                ),
            ).extractResult()

            val yieldContractAddress = HEX_PREFIX + rawContractAddress.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

            yieldContractAddress
        } else {
            storedYieldContractAddress
        }
    }

    override suspend fun getYieldSupplyStatus(tokenContractAddress: String): YieldSupplyStatus {
        val result = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldContract(),
                data = EthereumYieldSupplyStatusCallData(tokenContractAddress).dataHex,
            ),
        ).extractResult()
        return ethereumYieldSupplyStatusConverter.convert(result)
    }

    override suspend fun getBalance(yieldSupplyStatus: YieldSupplyStatus, token: Token): Amount = coroutineScope {
        val balanceDeferred = async {
            val rawBalance = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = getYieldContract(),
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

        Amount(
            value = balanceDeferred.await(),
            blockchain = wallet.blockchain,
            currencySymbol = token.symbol,
            type = AmountType.TokenYieldSupply(
                token = token,
                isActive = yieldSupplyStatus.isActive,
                isInitialized = yieldSupplyStatus.isInitialized,
                isAllowedToSpend = isAllowedToSpendDeferred.await(),
            ),
        )
    }

    override suspend fun getProtocolBalance(token: Token): BigDecimal {
        val rawProtocolBalance = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldContract(),
                data = EthereumYieldSupplyProtocolBalanceCallData(token.contractAddress).dataHex,
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
                    spenderAddress = getYieldContract(),
                ).dataHex,
            ),
        ).extractResult()

        val allowanceValue = EthereumUtils.parseEthereumDecimal(
            value = allowanceRaw,
            decimalsCount = wallet.blockchain.decimals(),
        ).orZero()
        val tokenBalance = wallet.getTokenAmount(token)?.value.orZero()

        return allowanceValue > BigDecimal.ZERO && allowanceValue >= tokenBalance
    }

    private fun storeKey(providerType: YieldSupplyProviderType) = "yield-supply-${providerType.key}" +
        "-${wallet.publicKey.blockchainKey.toCompressedPublicKey().toHexString()}" +
        "-${wallet.address}"

    private fun Result<JsonRPCResponse>.extractResult(): String = extractResult(adapter = stringAdapter)

    private fun <Body> Result<JsonRPCResponse>.extractResult(adapter: JsonAdapter<Body>): Body {
        return when (this) {
            is Result.Success -> {
                runCatching { adapter.fromJsonValue(data.result) }.getOrNull()
                    ?: throw data.error?.let { error ->
                        BlockchainSdkError.Ethereum.Api(code = error.code, message = error.message)
                    } ?: BlockchainSdkError.CustomError("Unknown response format")
            }
            is Result.Failure -> {
                throw error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
            }
        }
    }

    private companion object {
        const val BASIS_POINTS_DECIMALS = 4
    }
}