package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank

private const val AVALANCHE_POSTFIX = "ext/bc/C/rpc"

internal fun Blockchain.getEthereumJsonRpcProviders(
    config: BlockchainSdkConfig,
): List<EthereumJsonRpcProvider> {
    val providers = when (this) {
        Blockchain.Arbitrum -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://arb1.arbitrum.io/rpc/"),
            getInfuraProvider(baseUrl = "https://arbitrum-mainnet.infura.io/v3/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://node.offchainlabs.com:8547/")
        )
        Blockchain.ArbitrumTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli-rollup.arbitrum.io/rpc/")
        )
        Blockchain.Avalanche -> listOfNotNull(
            // postfix is required because the AVALANCHE API needs url without a last slash !!!
            config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
                EthereumJsonRpcProvider(
                    baseUrl = "https://avax.nownodes.io/",
                    postfixUrl = AVALANCHE_POSTFIX,
                    nowNodesApiKey = nowNodesApiKey // special for Avalanche
                )
            },
            config.getBlockCredentials?.apiKey.letNotBlank { getBlockApiKey ->
                EthereumJsonRpcProvider(
                    baseUrl = "https://avax.getblock.io/mainnet/",
                    postfixUrl = AVALANCHE_POSTFIX,
                    getBlockApiKey = getBlockApiKey
                )
            },
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax.network/",
                postfixUrl = AVALANCHE_POSTFIX
            )
        )
        Blockchain.AvalancheTestnet -> listOf(
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax-test.network/",
                postfixUrl = AVALANCHE_POSTFIX
            )
        )
        Blockchain.Ethereum -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://eth.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://eth.getblock.io/mainnet/", config = config),
            getInfuraProvider(baseUrl = "https://mainnet.infura.io/v3/", config = config)
        )
        Blockchain.EthereumTestnet -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://eth-goerli.nownodes.io/", config = config),
            getInfuraProvider(baseUrl = "https://goerli.infura.io/v3/", config = config)
        )
        Blockchain.EthereumClassic -> listOfNotNull(
            getGetBlockProvider(baseUrl = "https://etc.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://www.ethercluster.com/etc/"),
            EthereumJsonRpcProvider(baseUrl = "https://etc.etcdesktop.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://blockscout.com/etc/mainnet/api/eth-rpc/"),
            EthereumJsonRpcProvider(baseUrl = "https://etc.mytokenpocket.vip/"),
            EthereumJsonRpcProvider(baseUrl = "https://besu.etc-network.info/"),
            EthereumJsonRpcProvider(baseUrl = "https://geth.etc-network.info/"),
        )
        Blockchain.EthereumClassicTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://www.ethercluster.com/kotti/")
        )
        Blockchain.Fantom -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://ftm.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://ftm.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ftm.tools/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpcapi.fantom.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/", postfixUrl = "fantom"),
            EthereumJsonRpcProvider(baseUrl = "https://ftmrpc.ultimatenodes.io/"),
        )
        Blockchain.FantomTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.testnet.fantom.network/"),
        )
        Blockchain.RSK -> listOfNotNull(
            getGetBlockProvider(baseUrl = "https://rsk.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://public-node.rsk.co/")
        )
        Blockchain.BSC -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://bsc.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://bsc.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://bsc-dataseed.binance.org/"),
            config.quickNodeBscCredentials?.let { credentials ->
                if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                    EthereumJsonRpcProvider(
                        baseUrl = "https://${credentials.subdomain}.bsc.discover.quiknode.pro/" +
                            "${credentials.apiKey}/"
                    )
                } else {
                    null
                }
            }
        )
        Blockchain.BSCTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://data-seed-prebsc-1-s1.binance.org:8545/")
        )
        Blockchain.Polygon -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://matic.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://matic.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://polygon-rpc.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.matic.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://matic-mainnet.chainstacklabs.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.maticvigil.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.matic.quiknode.pro/"),
            EthereumJsonRpcProvider(baseUrl = "https://matic-mainnet-full-rpc.bwarelabs.com/"),
        )
        Blockchain.PolygonTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mumbai.maticvigil.com/")
        )
        Blockchain.Gnosis -> listOfNotNull(
            getGetBlockProvider(baseUrl = "https://gno.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.gnosischain.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://gnosischain-rpc.gateway.pokt.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/gnosis/"),
            EthereumJsonRpcProvider(baseUrl = "https://gnosis-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://xdai-rpc.gateway.pokt.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://xdai-archive.blockscout.com/"),
        )
        Blockchain.Optimism -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://optimism.nownodes.io/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.optimism.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://optimism-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/optimism/"),
        )
        Blockchain.OptimismTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli.optimism.io/"),
        )
        Blockchain.EthereumFair -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.etherfair.org/")
        )
        Blockchain.EthereumPow -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://ethw.nownodes.io/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.ethereumpow.org/")
        )
        Blockchain.EthereumPowTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://iceberg.ethereumpow.org/")
        )
        Blockchain.SaltPay -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.bicoccachain.net/", authToken = config.saltPayAuthToken)
        )
        Blockchain.Kava -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://evm.kava.io"),
            EthereumJsonRpcProvider(baseUrl = "https://evm2.kava.io"),
        )
        Blockchain.KavaTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://evm.testnet.kava.io"),
        )
        else -> throw IllegalStateException("$this isn't supported")
    }

    if (providers.isEmpty()) throw IllegalStateException("Provider list of $this is null or empty")

    return providers
}

private fun getNowNodesProvider(
    baseUrl: String,
    config: BlockchainSdkConfig,
): EthereumJsonRpcProvider? {
    return config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = nowNodesApiKey)
    }
}

private fun getGetBlockProvider(
    baseUrl: String,
    config: BlockchainSdkConfig,
): EthereumJsonRpcProvider? {
    return config.getBlockCredentials?.apiKey.letNotBlank { getBlockApiKey ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, getBlockApiKey = getBlockApiKey)
    }
}

private fun getInfuraProvider(
    baseUrl: String,
    config: BlockchainSdkConfig,
): EthereumJsonRpcProvider? {
    return config.infuraProjectId.letNotBlank { infuraProjectId ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = infuraProjectId)
    }
}