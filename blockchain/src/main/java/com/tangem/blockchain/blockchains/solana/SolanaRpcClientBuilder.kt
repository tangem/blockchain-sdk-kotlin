package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import okhttp3.Interceptor
import org.p2p.solanaj.rpc.Cluster

internal class SolanaRpcClientBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SolanaRpcClient>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Solana, Blockchain.SolanaTestnet)

    override fun createProviders(blockchain: Blockchain): List<SolanaRpcClient> {
        return if (blockchain.isTestnet()) {
            listOf(devNet())
        } else {
            listOfNotNull(
                config.nowNodeCredentials?.apiKey?.letNotBlank { getNowNodesProvider(config.nowNodeCredentials) },
                config.quickNodeSolanaCredentials?.let(::getQuickNodeProvider),
                mainNet(),
            )
        }
    }

    private fun devNet(): SolanaRpcClient = SolanaRpcClient(Cluster.DEVNET.endpoint)

    private fun mainNet(): SolanaRpcClient = SolanaRpcClient(Cluster.MAINNET.endpoint)

    private fun getNowNodesProvider(cred: NowNodeCredentials): SolanaRpcClient {
        return SolanaRpcClient(
            baseUrl = "https://sol.nownodes.io",
            httpInterceptors = createInterceptor(NowNodeCredentials.headerApiKey, cred.apiKey),
        )
    }

    private fun getQuickNodeProvider(credentials: QuickNodeCredentials): SolanaRpcClient? {
        return if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
            val baseUrl = "https://${credentials.subdomain}.solana-mainnet.discover.quiknode.pro/${credentials.apiKey}"
            SolanaRpcClient(baseUrl = baseUrl)
        } else {
            null
        }
    }

    private fun createInterceptor(key: String, value: String): List<Interceptor> {
        return listOf(AddHeaderInterceptor(mapOf(key to value)))
    }

    @Suppress("UnusedPrivateMember")
    private fun testNet(): SolanaRpcClient = SolanaRpcClient(Cluster.TESTNET.endpoint)

    // contains old data about 7 hours
    @Suppress("UnusedPrivateMember")
    private fun ankr(): SolanaRpcClient {
        return SolanaRpcClient(baseUrl = "https://rpc.ankr.com/solana")
    }

    // unstable
    @Suppress("UnusedPrivateMember")
    private fun getBlock(cred: GetBlockCredentials): SolanaRpcClient {
        return SolanaRpcClient(baseUrl = "https://go.getblock.io/${cred.solana}")
    }

    // zero uptime
    @Suppress("UnusedPrivateMember")
    private fun projectserum(): SolanaRpcClient {
        return SolanaRpcClient(baseUrl = "https://solana-api.projectserum.com")
    }
}