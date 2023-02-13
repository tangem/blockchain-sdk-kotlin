package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.*

private const val BSC_QUICKNODE_BASE_URL = "https://%1\$s.bsc.discover.quiknode.pro/%2\$s/"

internal fun Blockchain.getEthereumJsonRpcProviders(
    config: BlockchainSdkConfig
): List<EthereumJsonRpcProvider> {
    return when (this) {
        Blockchain.Arbitrum -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = API_ARBITRUM),
            getInfuraProvider(baseUrl = API_ARBITRUM_INFURA, config = config),
            EthereumJsonRpcProvider(baseUrl = API_ARBITRUM_OFFCHAIN)
        )
        Blockchain.ArbitrumTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_ARBITRUM_TESTNET)
        )
        Blockchain.Avalanche -> listOfNotNull(
            // special for Avalanche
            config.nowNodeCredentials?.apiKey?.let { nowNodesApiKey ->
                EthereumJsonRpcProvider(
                    baseUrl = API_AVALANCHE_NOWNODES,
                    nowNodesApiKey = nowNodesApiKey
                )
            },
            getGetBlockProvider(baseUrl = API_AVALANCHE_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_AVALANCHE)
        )
        Blockchain.AvalancheTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_AVALANCHE_TESTNET)
        )
        Blockchain.Ethereum -> listOfNotNull(
            getNowNodesProvider(API_ETH_NOWNODES, config),
            getGetBlockProvider(API_ETH_GETBLOCK, config),
            getInfuraProvider(API_ETH, config)
        )
        Blockchain.EthereumTestnet -> listOfNotNull(
            getNowNodesProvider(baseUrl = API_ETH_NOWNODES_TESTNET, config = config),
            getInfuraProvider(baseUrl = API_ETH_TESTNET, config = config)
        )
        Blockchain.EthereumClassic -> listOfNotNull(
            getGetBlockProvider(baseUrl = API_ETH_CLASSIC_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_CLUSTER),
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_ETCDESKTOP),
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_BLOCKSCOUT),
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_MYTOKEN),
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_BESU),
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_GETH),
        )
        Blockchain.EthereumClassicTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_ETH_CLASSIC_CLUSTER_TESTNET)
        )
        Blockchain.Fantom -> listOfNotNull(
            getNowNodesProvider(baseUrl = API_FANTOM_NOWNODES, config = config),
            getGetBlockProvider(baseUrl = API_FANTOM_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_FANTOM_TOOLS),
            EthereumJsonRpcProvider(baseUrl = API_FANTOM_NETWORK),
            EthereumJsonRpcProvider(baseUrl = API_FANTOM_ANKR_TOOLS),
            EthereumJsonRpcProvider(baseUrl = API_FANTOM_ULTIMATENODES),
        )
        Blockchain.FantomTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_FANTOM_TESTNET),
        )
        Blockchain.RSK -> listOfNotNull(
            getGetBlockProvider(baseUrl = API_RSK_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_RSK)
        )
        Blockchain.BSC -> listOfNotNull(
            getNowNodesProvider(baseUrl = API_BSC_NOWNODES, config = config),
            getGetBlockProvider(baseUrl = API_BSC_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_BSC),
            config.quickNodeBscCredentials?.let {
                EthereumJsonRpcProvider(
                    baseUrl = String.format(BSC_QUICKNODE_BASE_URL, it.subdomain, it.apiKey)
                )
            }
        )
        Blockchain.BSCTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_BSC_TESTNET)
        )
        Blockchain.Polygon -> listOfNotNull(
            getNowNodesProvider(baseUrl = API_POLYGON_NOWNODES, config = config),
            getGetBlockProvider(baseUrl = API_POLYGON_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_POLYGON),
            EthereumJsonRpcProvider(baseUrl = API_POLYGON_MATIC),
            EthereumJsonRpcProvider(baseUrl = API_POLYGON_CHAINSTACKLABS),
            EthereumJsonRpcProvider(baseUrl = API_POLYGON_MATICVIGIL),
            EthereumJsonRpcProvider(baseUrl = API_POLYGON_QUICKNODE),
            EthereumJsonRpcProvider(baseUrl = API_POLYGON_BWARELABS),
        )
        Blockchain.PolygonTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_POLYGON_TESTNET)
        )
        Blockchain.Gnosis -> listOfNotNull(
            getGetBlockProvider(baseUrl = API_GNOSIS_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_GNOSIS_CHAIN),
            EthereumJsonRpcProvider(baseUrl = API_GNOSIS_POKT),
            EthereumJsonRpcProvider(baseUrl = API_GNOSIS_ANKR),
            EthereumJsonRpcProvider(baseUrl = API_GNOSIS_BLAST),
            EthereumJsonRpcProvider(baseUrl = API_XDAI_POKT),
            EthereumJsonRpcProvider(baseUrl = API_XDAI_BLOCKSCOUT),
        )
        Blockchain.Optimism -> listOfNotNull(
            getNowNodesProvider(baseUrl = API_OPTIMISM_NOWNODES, config = config),
            getGetBlockProvider(baseUrl = API_OPTIMISM_GETBLOCK, config = config),
            EthereumJsonRpcProvider(baseUrl = API_OPTIMISM),
            EthereumJsonRpcProvider(baseUrl = API_OPTIMISM_BLAST),
            EthereumJsonRpcProvider(baseUrl = API_OPTIMISM_ANKR),
        )
        Blockchain.OptimismTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_OPTIMISM_TESTNET),
        )
        Blockchain.EthereumFair -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_ETH_FAIR_RPC)
        )
        Blockchain.EthereumPow -> listOfNotNull(
            getNowNodesProvider(baseUrl = API_ETH_POW_NOWNODES, config = config),
            EthereumJsonRpcProvider(baseUrl = API_ETH_POW)
        )
        Blockchain.EthereumPowTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_ETH_POW_TESTNET)
        )
        Blockchain.SaltPay -> listOf(
            EthereumJsonRpcProvider(baseUrl = API_SALTPAY)
        )
        else -> throw IllegalStateException("$this isn't supported")
    }
}

private fun getNowNodesProvider(
    baseUrl: String,
    config: BlockchainSdkConfig
): EthereumJsonRpcProvider? {
    return config.nowNodeCredentials?.apiKey?.let { nowNodesApiKey ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = nowNodesApiKey)
    }
}

private fun getGetBlockProvider(
    baseUrl: String,
    config: BlockchainSdkConfig
): EthereumJsonRpcProvider? {
    return config.getBlockCredentials?.apiKey?.let { apiKey ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, getBlockApiKey = apiKey)
    }
}

private fun getInfuraProvider(
    baseUrl: String,
    config: BlockchainSdkConfig
): EthereumJsonRpcProvider? {
    return config.infuraProjectId?.let { infuraProjectId ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = infuraProjectId)
    }
}