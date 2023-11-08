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
            getNowNodesProvider(baseUrl = "https://arbitrum.nownodes.io/", config = config),
            getInfuraProvider(baseUrl = "https://arbitrum-mainnet.infura.io/v3/", config = config)
        )
        Blockchain.ArbitrumTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli-rollup.arbitrum.io/rpc/")
        )
        Blockchain.Avalanche -> listOfNotNull(
            // postfix is required because the AVALANCHE API needs url without a last slash !!!
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax.network/",
                postfixUrl = AVALANCHE_POSTFIX
            ),
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
            getInfuraProvider(baseUrl = "https://mainnet.infura.io/v3/", config = config),
        )
        Blockchain.EthereumTestnet -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://eth-goerli.nownodes.io/", config = config),
            getInfuraProvider(baseUrl = "https://goerli.infura.io/v3/", config = config)
        )
        Blockchain.EthereumClassic -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://etc.etcdesktop.com/"),
            getGetBlockProvider(baseUrl = "https://etc.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/etc/"),
            EthereumJsonRpcProvider(baseUrl = "https://blockscout.com/etc/mainnet/api/eth-rpc/"),
            EthereumJsonRpcProvider(baseUrl = "https://etc.mytokenpocket.vip/"),
            EthereumJsonRpcProvider(baseUrl = "https://besu-de.etc-network.info/"),
            EthereumJsonRpcProvider(baseUrl = "https://geth-at.etc-network.info/"),
        )
        Blockchain.EthereumClassicTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/kotti/")
        )
        Blockchain.Fantom -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://ftm.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://ftm.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ftm.tools/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpcapi.fantom.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://fantom-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://fantom-rpc.gateway.pokt.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/", postfixUrl = "fantom"),
        )
        Blockchain.FantomTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.testnet.fantom.network/"),
        )
        Blockchain.RSK -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://public-node.rsk.co/"),
            getNowNodesProvider(baseUrl = "https://rsk.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://rsk.getblock.io/mainnet/", config = config),
        )
        // https://docs.fantom.foundation/api/public-api-endpoints
        Blockchain.BSC -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://bsc-dataseed.binance.org/"),
            getNowNodesProvider(baseUrl = "https://bsc.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://bsc.getblock.io/mainnet/", config = config),
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
        // https://wiki.polygon.technology/docs/operate/network-rpc-endpoints
        Blockchain.Polygon -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://polygon-rpc.com/"),
            getNowNodesProvider(baseUrl = "https://matic.nownodes.io/", config = config),
            getGetBlockProvider(baseUrl = "https://matic.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.maticvigil.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.matic.quiknode.pro/"),
        )
        Blockchain.PolygonTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mumbai.maticvigil.com/")
        )
        Blockchain.Gnosis -> listOfNotNull(
            getGetBlockProvider(baseUrl = "https://gno.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.gnosischain.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://gnosischain-rpc.gateway.pokt.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://gnosis-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://xdai-rpc.gateway.pokt.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/gnosis/"),
        )
        Blockchain.Optimism -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.optimism.io/"),
            getNowNodesProvider(baseUrl = "https://optimism.nownodes.io/", config = config),
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
        Blockchain.Kava -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://evm.kava.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://evm2.kava.io/"),
        )
        Blockchain.KavaTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://evm.testnet.kava.io/"),
        )
        Blockchain.Cronos -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://evm.cronos.org/"),
            EthereumJsonRpcProvider(baseUrl = "https://evm-cronos.crypto.org/"),
            getGetBlockProvider(baseUrl = "https://cro.getblock.io/mainnet/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://node.croswap.com/rpc/"),
            EthereumJsonRpcProvider(baseUrl = "https://cronos.blockpi.network/v1/rpc/public/"),
            EthereumJsonRpcProvider(baseUrl = "https://cronos-evm.publicnode.com/"),
        )
        Blockchain.Telos -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.telos.net", postfixUrl = "evm"),
            EthereumJsonRpcProvider(baseUrl = "https://api.kainosbp.com", postfixUrl = "evm"),
            EthereumJsonRpcProvider(baseUrl = "https://telos-evm.rpc.thirdweb.com/")
        )

        Blockchain.TelosTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://telos-evm-testnet.rpc.thirdweb.com/"),
        )

        Blockchain.OctaSpace -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.octa.space"),
            EthereumJsonRpcProvider(baseUrl = "https://octaspace.rpc.thirdweb.com"),
        )

        Blockchain.Decimal -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://node.decimalchain.com/web3/"),
            EthereumJsonRpcProvider(baseUrl = "https://node1-mainnet.decimalchain.com/web3/"),
            EthereumJsonRpcProvider(baseUrl = "https://node2-mainnet.decimalchain.com/web3/"),
            EthereumJsonRpcProvider(baseUrl = "https://node3-mainnet.decimalchain.com/web3/"),
            EthereumJsonRpcProvider(baseUrl = "https://node4-mainnet.decimalchain.com/web3/"),
        )

        Blockchain.DecimalTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://testnet-val.decimalchain.com/web3/")
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
