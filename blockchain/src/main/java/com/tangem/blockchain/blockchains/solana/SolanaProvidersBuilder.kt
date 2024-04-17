package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import org.p2p.solanaj.rpc.Cluster

internal class SolanaProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SolanaRpcClient>() {

    override fun createProviders(blockchain: Blockchain): List<SolanaRpcClient> {
        return providerTypes.mapNotNull {
            when (it) {
                ProviderType.NowNodes -> getNowNodesProvider()
                ProviderType.QuickNode -> getQuickNodeProvider()
                ProviderType.Solana.Official -> mainNet()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<SolanaRpcClient> = listOf(devNet())

    private fun devNet(): SolanaRpcClient = SolanaRpcClient(Cluster.DEVNET.endpoint)

    private fun mainNet(): SolanaRpcClient = SolanaRpcClient(Cluster.MAINNET.endpoint)

    private fun getNowNodesProvider(): SolanaRpcClient? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            SolanaRpcClient(
                baseUrl = "https://sol.nownodes.io",
                httpInterceptors = listOf(
                    AddHeaderInterceptor(mapOf(NowNodeCredentials.headerApiKey to it)),
                ),
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