package com.tangem.blockchain.test

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.InkProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumLegacyTransactionBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.card.EllipticCurve
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal

/**
 * Test to debug Ink network fee calculation
 */
fun main() {
    println("=== Starting Ink Fee Calculation Test ===\n")
    
    // Initialize DepsContainer
    val config = BlockchainSdkConfig(
        nowNodeCredentials = NowNodeCredentials(apiKey = null),
        getBlockCredentials = GetBlockCredentials(
            ink = GetBlockAccessToken(jsonRpc = null),
        ),
    )
    
    DepsContainer.onInit(
        config = config,
        featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false),
    )
    
    println("1. Creating Ink providers...")
    val providerTypes = listOf(
        ProviderType.Public(url = "https://rpc-gel.inkonchain.com/"),
        ProviderType.Public(url = "https://rpc-qnd.inkonchain.com/"),
    )
    
    val providersBuilder = InkProvidersBuilder(providerTypes, config)
    val providers = providersBuilder.build(Blockchain.Ink)
    println("   Providers created: ${providers.size}")
    providers.forEachIndexed { index, provider ->
        println("   Provider $index: ${provider.baseUrl}")
    }
    
    if (providers.isEmpty()) {
        println("\n❌ ERROR: No providers created! This is why fees can't be calculated.")
        return
    }
    
    println("\n2. Creating MultiNetworkProvider...")
    val multiNetworkProvider = MultiNetworkProvider(providers)
    
    println("\n3. Creating EthereumNetworkService...")
    val networkService = EthereumNetworkService(
        multiJsonRpcProvider = multiNetworkProvider,
        yieldSupplyProvider = null,
    )
    
    println("\n4. Creating Wallet...")
    val wallet = Wallet(
        blockchain = Blockchain.Ink,
        addresses = setOf(Address("0x823853455012801dfa2b9874d95a66c57b7013b1")),
        publicKey = Wallet.PublicKey(
            seedKey = ByteArray(65) { 0x04 },
            derivationType = Wallet.PublicKey.DerivationType.Plain(EllipticCurve.Secp256k1),
        ),
        tokens = emptySet(),
    )
    
    println("\n5. Creating EthereumOptimisticRollupWalletManager...")
    val walletManager = EthereumOptimisticRollupWalletManager(
        wallet = wallet,
        transactionBuilder = EthereumLegacyTransactionBuilder(wallet),
        networkProvider = networkService,
        nftProvider = null,
        transactionHistoryProvider = null,
        yieldSupplyProvider = null,
    )
    
    println("\n6. Testing basic RPC calls...")
    runBlocking {
        // Test gas price
        print("   Testing eth_gasPrice... ")
        val gasPriceResult = networkService.getGasPrice()
        when (gasPriceResult) {
            is Result.Success -> println("✅ Success: ${gasPriceResult.data} wei")
            is Result.Failure -> println("❌ Failed: ${gasPriceResult.error}")
        }
        
        // Test gas limit estimation
        print("   Testing eth_estimateGas... ")
        val gasLimitResult = networkService.getGasLimit(
            to = "0xfe45cf5283052a1be7ec256a76059bde3e9dfde3",
            from = wallet.address,
            value = "0x2386f26fc10000", // 0.01 ETH
            data = null,
        )
        when (gasLimitResult) {
            is Result.Success -> println("✅ Success: ${gasLimitResult.data} gas")
            is Result.Failure -> println("❌ Failed: ${gasLimitResult.error}")
        }
    }
    
    println("\n7. Testing fee calculation for ETH transfer...")
    val ethAmount = Amount(
        value = BigDecimal("0.01"),
        blockchain = Blockchain.Ink,
        type = AmountType.Coin,
    )
    
    runBlocking {
        val feeResult = walletManager.getFee(
            amount = ethAmount,
            destination = "0xfe45cf5283052a1be7ec256a76059bde3e9dfde3",
            callData = null,
        )
        
        when (feeResult) {
            is Result.Success -> {
                val fee = feeResult.data as? TransactionFee.Choosable
                println("✅ Fee calculation SUCCESS!")
                println("   Normal fee: ${fee?.normal?.amount?.value} ETH")
                println("   Gas limit: ${(fee?.normal as? com.tangem.blockchain.common.transaction.Fee.Ethereum.Legacy)?.gasLimit}")
            }
            is Result.Failure -> {
                println("❌ Fee calculation FAILED!")
                println("   Error: ${feeResult.error}")
                println("   Error type: ${feeResult.error::class.simpleName}")
            }
        }
    }
    
    println("\n8. Testing fee calculation for WETH token transfer...")
    val wethToken = Token(
        symbol = "WETH",
        contractAddress = "0x4200000000000000000000000000000000000006",
        decimals = 18,
    )
    
    val wethAmount = Amount(
        value = BigDecimal("0.001"),
        blockchain = Blockchain.Ink,
        type = AmountType.Token(wethToken),
    )
    
    runBlocking {
        val feeResult = walletManager.getFee(
            amount = wethAmount,
            destination = "0xfe45cf5283052a1be7ec256a76059bde3e9dfde3",
            callData = null,
        )
        
        when (feeResult) {
            is Result.Success -> {
                val fee = feeResult.data as? TransactionFee.Choosable
                println("✅ WETH fee calculation SUCCESS!")
                println("   Normal fee: ${fee?.normal?.amount?.value} ETH")
                println("   Gas limit: ${(fee?.normal as? com.tangem.blockchain.common.transaction.Fee.Ethereum.Legacy)?.gasLimit}")
            }
            is Result.Failure -> {
                println("❌ WETH fee calculation FAILED!")
                println("   Error: ${feeResult.error}")
                println("   Error type: ${feeResult.error::class.simpleName}")
                
                // Print stack trace if available
                if (feeResult.error is BlockchainSdkError.WrappedThrowable) {
                    println("   Wrapped error: ${(feeResult.error as BlockchainSdkError.WrappedThrowable).throwable}")
                    (feeResult.error as BlockchainSdkError.WrappedThrowable).throwable.printStackTrace()
                }
            }
        }
    }
    
    println("\n=== Test Complete ===")
}

