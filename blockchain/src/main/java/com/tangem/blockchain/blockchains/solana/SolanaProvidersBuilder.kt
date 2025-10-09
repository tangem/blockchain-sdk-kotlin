package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.interceptors.HttpLoggingInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import okhttp3.Interceptor
import org.p2p.solanaj.rpc.Cluster

internal class SolanaProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SolanaRpcClient>() {

    override fun createProviders(blockchain: Blockchain): List<SolanaRpcClient> {
        return providerTypes.mapNotNull {
            when (it) {
                ProviderType.Blink -> getBlinkProvider()
                ProviderType.NowNodes -> getNowNodesProvider()
                ProviderType.QuickNode -> getQuickNodeProvider()
                ProviderType.GetBlock -> getGetBlock()
                ProviderType.Solana.Official -> mainNet()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<SolanaRpcClient> = listOf(devNet())

    private fun devNet(): SolanaRpcClient = SolanaRpcClient(Cluster.DEVNET.endpoint)

    private fun mainNet(): SolanaRpcClient = SolanaRpcClient(
        baseUrl = Cluster.MAINNET.endpoint,
        httpInterceptors = createLoggingInterceptors(),
    )

    private fun getNowNodesProvider(): SolanaRpcClient? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            SolanaRpcClient(
                baseUrl = "https://sol.nownodes.io",
                httpInterceptors = listOf(
                    AddHeaderInterceptor(mapOf(NowNodeCredentials.headerApiKey to it)),
                    *createLoggingInterceptors().toTypedArray(),
                ),
            )
        }
    }

    private fun getBlinkProvider(): SolanaRpcClient? {
        return config.blinkApiKey?.letNotBlank { blinkApiKey ->
            SolanaRpcClient(
                baseUrl = "https://sol.blinklabs.xyz/v1/$blinkApiKey",
                skipPreflight = true,
                httpInterceptors = createLoggingInterceptors(),
            )
        }
    }

    private fun getQuickNodeProvider(): SolanaRpcClient? {
        return config.quickNodeSolanaCredentials?.let { creds ->
            if (creds.subdomain.isNotBlank() && creds.apiKey.isNotBlank()) {
                SolanaRpcClient(
                    baseUrl = "https://${creds.subdomain}/${creds.apiKey}",
                    httpInterceptors = createLoggingInterceptors(),
                )
            } else {
                null
            }
        }
    }

    private fun getGetBlock(): SolanaRpcClient? {
        return config.getBlockCredentials?.solana?.jsonRpc?.let { accessToken ->
            if (accessToken.isNotBlank()) {
                SolanaRpcClient(
                    baseUrl = "https://go.getblock.io/$accessToken",
                    httpInterceptors = createLoggingInterceptors(),
                )
            } else {
                null
            }
        }
    }

    private fun createLoggingInterceptors(): List<Interceptor> {
        return listOf(
            *BlockchainSdkRetrofitBuilder.interceptors.toTypedArray(),
            HttpLoggingInterceptor,
        )
    }

    @Suppress("UnusedPrivateMember")
    private fun testNet(): SolanaRpcClient = SolanaRpcClient(Cluster.TESTNET.endpoint)

    // contains old data about 7 hours
    @Suppress("UnusedPrivateMember")
    private fun ankr(): SolanaRpcClient {
        return SolanaRpcClient(baseUrl = "https://rpc.ankr.com/solana")
    }

    // zero uptime
    @Suppress("UnusedPrivateMember")
    private fun projectserum(): SolanaRpcClient {
        return SolanaRpcClient(baseUrl = "https://solana-api.projectserum.com")
    }
}