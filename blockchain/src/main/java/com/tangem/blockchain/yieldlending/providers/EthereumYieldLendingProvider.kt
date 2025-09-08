package com.tangem.blockchain.yieldlending.providers

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.AllowanceERC20TokenCallData
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainSavedData.YieldLendingModule
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.orZero
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.moshi
import com.tangem.blockchain.yieldlending.YieldLendingContractAddressFactory
import com.tangem.blockchain.yieldlending.YieldLendingProvider
import com.tangem.blockchain.yieldlending.providers.ethereum.EthereumLendingStatus
import com.tangem.blockchain.yieldlending.providers.ethereum.converters.EthereumLendingStatusConverter
import com.tangem.blockchain.yieldlending.providers.ethereum.factory.EthereumYieldLendingModuleCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.processor.EthereumYieldLendingServiceFeeCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingBalanceCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingProtocolBalanceCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingStatusCallData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

@OptIn(ExperimentalStdlibApi::class)
internal class EthereumYieldLendingProvider(
    private val multiJsonRpcProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    private val wallet: Wallet,
    private val contractAddressFactory: YieldLendingContractAddressFactory,
    private val dataStorage: AdvancedDataStorage,
) : YieldLendingProvider {

    private val stringAdapter by lazy { moshi.adapter<String>() }
    private val ethereumLendingStatusConverter by lazy {
        EthereumLendingStatusConverter(wallet.blockchain.decimals())
    }

    override val factoryContractAddress: String = contractAddressFactory.getFactoryAddress()
    override val processorContractAddress: String = contractAddressFactory.getProcessorAddress()

    override suspend fun getServiceFee(): BigDecimal {
        return multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = contractAddressFactory.getProcessorAddress(),
                data = EthereumYieldLendingServiceFeeCallData.dataHex,
            ),
        ).extractResult().hexToBigDecimal().movePointLeft(4)
    }

    override suspend fun getYieldContract(): String {
        val storedYieldContractAddress = dataStorage.getOrNull<YieldLendingModule>(wallet.address)
            ?.yieldContractAddress

        return if (storedYieldContractAddress == null) {
            val rawContractAddress = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = contractAddressFactory.getFactoryAddress(),
                    data = EthereumYieldLendingModuleCallData(wallet.address).dataHex,
                ),
            ).extractResult()

            val yieldContractAddress = HEX_PREFIX + rawContractAddress.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

            dataStorage.store(
                publicKey = wallet.publicKey,
                value = YieldLendingModule(yieldContractAddress),
            )

            yieldContractAddress
        } else {
            storedYieldContractAddress
        }
    }

    override suspend fun getLendingStatus(tokenContractAddress: String): EthereumLendingStatus {
        val result = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldContract(),
                data = EthereumYieldLendingStatusCallData(tokenContractAddress).dataHex,
            ),
        ).extractResult()
        return ethereumLendingStatusConverter.convert(result)
    }

    override suspend fun getLentBalance(token: Token): Amount = coroutineScope {
        val balanceDeferred = async {
            val rawBalance = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::call,
                data = EthCallObject(
                    to = getYieldContract(),
                    data = EthereumYieldLendingBalanceCallData(token.contractAddress).dataHex,
                ),
            )

            requireNotNull(
                EthereumUtils.parseEthereumDecimal(
                    rawBalance.extractResult(),
                    token.decimals,
                )
            ) { "Failed to parse token balance. Token: ${token.name}. Balance: ${rawBalance.extractResult()}" }
        }

        val yieldLendingStatusDeferred = async {
            getLendingStatus(token.contractAddress)
        }
        val isAllowedToSpendDeferred = async {
            isAllowedToSpend(token.contractAddress)
        }

        Amount(
            value = balanceDeferred.await(),
            blockchain = wallet.blockchain,
            currencySymbol = token.symbol,
            type = AmountType.YieldLend(
                token = token,
                isActive = yieldLendingStatusDeferred.await().isActive,
                isInitialized = yieldLendingStatusDeferred.await().isInitialized,
                isAllowedToSpend = isAllowedToSpendDeferred.await(),
            ),
        )
    }

    override suspend fun getProtocolBalance(token: Token): BigDecimal {
        val rawProtocolBalance = multiJsonRpcProvider.performRequest(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = getYieldContract(),
                data = EthereumYieldLendingProtocolBalanceCallData(token.contractAddress).dataHex,
            ),
        ).extractResult()

        return EthereumUtils.parseEthereumDecimal(
            rawProtocolBalance,
            token.decimals,
        ).orZero()
    }

    override suspend fun isLent(token: Token): Boolean {
        val protocolBalance = getProtocolBalance(token)

        return protocolBalance > BigDecimal.ZERO
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
}