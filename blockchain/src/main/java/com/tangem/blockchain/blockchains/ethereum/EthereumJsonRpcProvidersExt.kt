package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank

private const val AVALANCHE_POSTFIX = "ext/bc/C/rpc"

@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun Blockchain.getEthereumJsonRpcProviders(config: BlockchainSdkConfig): List<EthereumJsonRpcProvider> {
    val providers = when (this) {
        Blockchain.Arbitrum -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://arb1.arbitrum.io/rpc/"),
            getNowNodesProvider(baseUrl = "https://arbitrum.nownodes.io/", config = config),
            getInfuraProvider(baseUrl = "https://arbitrum-mainnet.infura.io/v3/", config = config),
        )

        Blockchain.ArbitrumTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli-rollup.arbitrum.io/rpc/"),
        )

        Blockchain.Avalanche -> listOfNotNull(
            // postfix is required because the AVALANCHE API needs url without a last slash !!!
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax.network/",
                postfixUrl = AVALANCHE_POSTFIX,
            ),
            config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
                EthereumJsonRpcProvider(
                    baseUrl = "https://avax.nownodes.io/",
                    postfixUrl = AVALANCHE_POSTFIX,
                    nowNodesApiKey = nowNodesApiKey, // special for Avalanche
                )
            },
            config.getBlockCredentials?.avalanche?.jsonRpc.letNotBlank { avalancheToken ->
                EthereumJsonRpcProvider(
                    baseUrl = "https://go.getblock.io/$avalancheToken/",
                    postfixUrl = AVALANCHE_POSTFIX,
                )
            },
        )
        Blockchain.AvalancheTestnet -> listOf(
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax-test.network/",
                postfixUrl = AVALANCHE_POSTFIX,
            ),
        )
        Blockchain.Ethereum -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://eth.nownodes.io/", config = config),
            config.getBlockCredentials?.eth?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
            getInfuraProvider(baseUrl = "https://mainnet.infura.io/v3/", config = config),
        )
        Blockchain.EthereumTestnet -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://eth-goerli.nownodes.io/", config = config),
            getInfuraProvider(baseUrl = "https://goerli.infura.io/v3/", config = config),
        )
        Blockchain.EthereumClassic -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://etc.etcdesktop.com/"),
            config.getBlockCredentials?.etc?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
            EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/etc/"),
            EthereumJsonRpcProvider(baseUrl = "https://etc.mytokenpocket.vip/"),
            EthereumJsonRpcProvider(baseUrl = "https://besu-de.etc-network.info/"),
            EthereumJsonRpcProvider(baseUrl = "https://geth-at.etc-network.info/"),
        )
        Blockchain.EthereumClassicTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/kotti/"),
        )
        Blockchain.Fantom -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://ftm.nownodes.io/", config = config),
            config.getBlockCredentials?.fantom?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ftm.tools/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpcapi.fantom.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://fantom-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/", postfixUrl = "fantom"),
        )
        Blockchain.FantomTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.testnet.fantom.network/"),
        )
        Blockchain.RSK -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://public-node.rsk.co/"),
            getNowNodesProvider(baseUrl = "https://rsk.nownodes.io/", config = config),
            config.getBlockCredentials?.rsk?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
        )
        // https://docs.fantom.foundation/api/public-api-endpoints
        Blockchain.BSC -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://bsc-dataseed.binance.org/"),
            getNowNodesProvider(baseUrl = "https://bsc.nownodes.io/", config = config),
            config.getBlockCredentials?.bsc?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
            config.quickNodeBscCredentials?.let { credentials ->
                if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                    EthereumJsonRpcProvider(
                        baseUrl = "https://${credentials.subdomain}.bsc.discover.quiknode.pro/" +
                            "${credentials.apiKey}/",
                    )
                } else {
                    null
                }
            },
        )
        Blockchain.BSCTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://data-seed-prebsc-1-s1.binance.org:8545/"),
        )
        // https://wiki.polygon.technology/docs/operate/network-rpc-endpoints
        Blockchain.Polygon -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://polygon-rpc.com/"),
            getNowNodesProvider(baseUrl = "https://matic.nownodes.io/", config = config),
            config.getBlockCredentials?.polygon?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.maticvigil.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.matic.quiknode.pro/"),
        )
        Blockchain.PolygonTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mumbai.maticvigil.com/"),
        )
        Blockchain.Gnosis -> listOfNotNull(
            config.getBlockCredentials?.gnosis?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
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
        Blockchain.Dischain -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.dischain.xyz/"),
        )
        Blockchain.EthereumPow -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://ethw.nownodes.io/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.ethereumpow.org/"),
        )
        Blockchain.EthereumPowTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://iceberg.ethereumpow.org/"),
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
            config.getBlockCredentials?.cronos?.jsonRpc.letNotBlank { getGetBlockProvider(accessToken = it) },
            EthereumJsonRpcProvider(baseUrl = "https://cronos.blockpi.network/v1/rpc/public/"),
            EthereumJsonRpcProvider(baseUrl = "https://cronos-evm.publicnode.com/"),
        )
        Blockchain.Telos -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.telos.net", postfixUrl = "evm"),
            EthereumJsonRpcProvider(baseUrl = "https://api.kainosbp.com", postfixUrl = "evm"),
            EthereumJsonRpcProvider(baseUrl = "https://telos-evm.rpc.thirdweb.com/"),
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
            EthereumJsonRpcProvider(baseUrl = "https://testnet-val.decimalchain.com/web3/"),
        )

        Blockchain.XDC -> listOfNotNull(
            getNowNodesProvider(baseUrl = "https://xdc.nownodes.io/", config = config),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.xdcrpc.com"),
            EthereumJsonRpcProvider(baseUrl = "https://erpc.xdcrpc.com"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.xinfin.network"),
            EthereumJsonRpcProvider(baseUrl = "https://erpc.xinfin.network"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.xdc.org"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/xdc/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc1.xinfin.network"),
        )

        Blockchain.XDCTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.apothem.network/"),
        )

        Blockchain.Playa3ull -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://api.mainnet.playa3ull.games/"),
        )

        Blockchain.Shibarium -> listOfNotNull(
            // the official api goes first due to the problems we have recently had with https://xdc.nownodes.io/
            EthereumJsonRpcProvider(baseUrl = "https://www.shibrpc.com/"),
            getNowNodesProvider(baseUrl = "https://shib.nownodes.io/", config = config),
        )

        Blockchain.ShibariumTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://puppynet.shibrpc.com/"),
        )

        Blockchain.Aurora -> listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.aurora.dev/"),
            EthereumJsonRpcProvider(baseUrl = "https://aurora.drpc.org/"),
            EthereumJsonRpcProvider(baseUrl = "https://1rpc.io/aurora/"),
        )

        Blockchain.AuroraTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://testnet.aurora.dev/"),
        )

        Blockchain.Areon -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://mainnet-rpc.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet-rpc2.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet-rpc3.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet-rpc4.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://mainnet-rpc5.areon.network/"),
        )

        Blockchain.AreonTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://testnet-rpc.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://testnet-rpc2.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://testnet-rpc3.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://testnet-rpc4.areon.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://testnet-rpc5.areon.network/"),
        )

        Blockchain.PulseChain -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.pulsechain.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://pulsechain.publicnode.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-pulsechain.g4mm4.io/"),
        )

        Blockchain.PulseChainTestnet -> listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.v4.testnet.pulsechain.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://pulsechain-testnet.publicnode.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-testnet-pulsechain.g4mm4.io/"),
        )

        else -> error("$this isn't supported")
    }

    if (providers.isEmpty()) error("Provider list of $this is null or empty")

    return providers
}

private fun getNowNodesProvider(baseUrl: String, config: BlockchainSdkConfig): EthereumJsonRpcProvider? {
    return config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = nowNodesApiKey)
    }
}

private fun getGetBlockProvider(accessToken: String): EthereumJsonRpcProvider =
    EthereumJsonRpcProvider(baseUrl = "https://go.getblock.io/$accessToken/")

private fun getInfuraProvider(baseUrl: String, config: BlockchainSdkConfig): EthereumJsonRpcProvider? {
    return config.infuraProjectId.letNotBlank { infuraProjectId ->
        EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = infuraProjectId)
    }
}