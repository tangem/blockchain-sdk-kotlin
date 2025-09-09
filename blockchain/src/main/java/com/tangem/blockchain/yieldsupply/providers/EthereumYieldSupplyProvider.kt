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
import com.tangem.blockchain.yieldsupply.YieldSupplyContractAddressFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchain.yieldsupply.providers.ethereum.converters.EthereumYieldSupplyStatusConverter
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyContractAddressCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyModuleCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.processor.EthereumYieldSupplyServiceFeeCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyProtocolBalanceCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyStatusCallData
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

    override fun factoryContractAddress(): String {
        return contractAddressFactory.getFactoryAddress()
    }

    override fun processorContractAddress(): String {
        return contractAddressFactory.getProcessorAddress()
    }

    override suspend fun getServiceFee(): BigDecimal {
        return multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = contractAddressFactory.getProcessorAddress(),
                data = EthereumYieldSupplyServiceFeeCallData.dataHex,
            ),
        ).extractResult().hexToBigDecimal().movePointLeft(BASIS_POINTS_DECIMALS)
    }

    override suspend fun getYieldContract(): String {
        val storedYieldContractAddress = dataStorage.getOrNull<YieldSupplyModule>(wallet.address)
            ?.yieldContractAddress

        return if (storedYieldContractAddress == null) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = contractAddressFactory.getFactoryAddress(),
                    data = EthereumYieldSupplyModuleCallData(wallet.address).dataHex,
                ),
            ).extractResult()

            val yieldContractAddress = HEX_PREFIX + rawContractAddress.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

            dataStorage.store(
                publicKey = wallet.publicKey,
                value = YieldSupplyModule(yieldContractAddress),
            )

            yieldContractAddress
        } else {
            storedYieldContractAddress
        }
    }

    override suspend fun calculateYieldContract(): String {
        val storedYieldContractAddress = dataStorage.getOrNull<YieldSupplyModule>(wallet.address)
            ?.yieldContractAddress

        return if (storedYieldContractAddress == null) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = contractAddressFactory.getFactoryAddress(),
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

    override suspend fun isAllowedToSpend(tokenContractAddress: String): Boolean {
        val allowed = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = tokenContractAddress,
                data = AllowanceERC20TokenCallData(
                    ownerAddress = wallet.address,
                    spenderAddress = getYieldContract(),
                ).dataHex,
            ),
        ).extractResult()

        return allowed != HEX_F.repeat(n = 64)
    }

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