package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import okhttp3.Interceptor
import org.p2p.solanaj.rpc.Cluster

internal class SolanaProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SolanaRpcClient>() {

    override fun createProviders(blockchain: Blockchain): List<SolanaRpcClient> {
        return listOfNotNull(
            getNowNodesProvider(),
            getQuickNodeProvider(),
            mainNet(),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<SolanaRpcClient> = listOf(devNet())

    private fun devNet(): SolanaRpcClient = SolanaRpcClient(Cluster.DEVNET.endpoint)

    private fun mainNet(): SolanaRpcClient = SolanaRpcClient(Cluster.MAINNET.endpoint)

    private fun getNowNodesProvider(): SolanaRpcClient? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            SolanaRpcClient(
                baseUrl = "https://sol.nownodes.io",
                httpInterceptors = createInterceptor(NowNodeCredentials.headerApiKey, it),
            )
        }
    }

    private fun getQuickNodeProvider(): SolanaRpcClient? {
        return config.quickNodeSolanaCredentials?.let { creds ->
            if (creds.subdomain.isNotBlank() && creds.apiKey.isNotBlank()) {
                SolanaRpcClient(
                    baseUrl = "https://${creds.subdomain}.solana-mainnet.discover.quiknode.pro/${creds.apiKey}",
                )
            } else {
                null
            }
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