package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.blockchain.extensions.AddHeaderInterceptor
import okhttp3.Interceptor
import org.p2p.solanaj.rpc.Cluster

/**
 * Created by Anton Zhilenkov on 06.02.2023.
 */
internal class SolanaRpcClientBuilder {

    fun build(isTestnet: Boolean, config: BlockchainSdkConfig): List<SolanaRpcClient> {
        return if (isTestnet) {
            listOf(devNet())
        } else {
            listOfNotNull(
                config.nowNodeCredentials?.let { nowNode(it) },
                config.quickNodeSolanaCredentials?.let { quickNode(it) },
                mainNet(),
            )
        }
    }

    private fun mainNet(): SolanaRpcClient = SolanaRpcClient(Cluster.MAINNET.endpoint)

    private fun devNet(): SolanaRpcClient = SolanaRpcClient(Cluster.DEVNET.endpoint)

    @Suppress("UnusedPrivateMember")
    private fun testNet(): SolanaRpcClient = SolanaRpcClient(Cluster.TESTNET.endpoint)

    private fun quickNode(cred: QuickNodeCredentials): SolanaRpcClient {
        val host = "https://${cred.subdomain}.solana-mainnet.discover.quiknode.pro/${cred.apiKey}"
        return SolanaRpcClient(host)
    }

    private fun nowNode(cred: NowNodeCredentials): SolanaRpcClient {
        return SolanaRpcClient(
            host = "https://sol.nownodes.io",
            httpInterceptors = createInterceptor(NowNodeCredentials.headerApiKey, cred.apiKey),
        )
    }

    // contains old data about 7 hours
    @Suppress("UnusedPrivateMember")
    private fun ankr(): SolanaRpcClient {
        return SolanaRpcClient(host = "https://rpc.ankr.com/solana")
    }

    // unstable
    @Suppress("UnusedPrivateMember")
    private fun getBlock(cred: GetBlockCredentials): SolanaRpcClient {
        return SolanaRpcClient(host = "https://go.getblock.io/${cred.solana}")
    }

    // zero uptime
    @Suppress("UnusedPrivateMember")
    private fun projectserum(): SolanaRpcClient {
        return SolanaRpcClient(host = "https://solana-api.projectserum.com")
    }

    private fun createInterceptor(key: String, value: String): List<Interceptor> {
        return listOf(AddHeaderInterceptor(mapOf(key to value)))
    }
}
