package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider

internal object EthereumLikeWalletManagerAssembly : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = getProvidersBuilder(blockchain, input.config).build(blockchain),
                ),
                transactionHistoryProvider = blockchain.getTransactionHistoryProvider(input.config),
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getProvidersBuilder(blockchain: Blockchain, config: BlockchainSdkConfig): EthereumLikeProvidersBuilder {
        return when (blockchain) {
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> ArbitrumProvidersBuilder(config)
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> AvalancheProvidersBuilder(config)
            Blockchain.EthereumTestnet, Blockchain.EthereumClassicTestnet -> EthereumClassicProvidersBuilder(config)
            Blockchain.Fantom, Blockchain.FantomTestnet -> FantomProvidersBuilder(config)
            Blockchain.RSK -> RSKProvidersBuilder(config)
            Blockchain.BSC, Blockchain.BSCTestnet -> BSCProvidersBuilder(config)
            Blockchain.Polygon, Blockchain.PolygonTestnet -> PolygonProvidersBuilder(config)
            Blockchain.Gnosis -> GnosisProvidersBuilder(config)
            Blockchain.Dischain -> DischainProvidersBuilder(config)
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> EthereumPowProvidersBuilder(config)
            Blockchain.Kava, Blockchain.KavaTestnet -> KavaProvidersBuilder(config)
            Blockchain.Cronos -> CronosProvidersBuilder(config)
            Blockchain.OctaSpace -> OctaSpaceProvidersBuilder(config)
            Blockchain.Playa3ull -> Playa3ullProvidersBuilder(config)
            Blockchain.Shibarium, Blockchain.ShibariumTestnet -> ShibariumProvidersBuilder(config)
            Blockchain.Aurora, Blockchain.AuroraTestnet -> AuroraProvidersBuilder(config)
            Blockchain.Areon, Blockchain.AreonTestnet -> AreonProvidersBuilder(config)
            Blockchain.PulseChain, Blockchain.PulseChainTestnet -> PulseChainProvidersBuilder(config)
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> ZkSyncEraProvidersBuilder(config)
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet -> MoonbeamProvidersBuilder(config)
            Blockchain.Manta, Blockchain.MantaTestnet -> MantaProvidersBuilder(config)
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet -> PolygonZkEVMProvidersBuilder(config)
            Blockchain.Base, Blockchain.BaseTestnet -> BaseProvidersBuilder(config)
            Blockchain.Moonriver, Blockchain.MoonriverTestnet -> MoonriverProvidersBuilder(config)
            Blockchain.Mantle, Blockchain.MantleTestnet -> MantleProvidersBuilder(config)
            Blockchain.Flare, Blockchain.FlareTestnet -> FlareProvidersBuilder(config)
            Blockchain.Taraxa, Blockchain.TaraxaTestnet -> TaraxaProvidersBuilder(config)
            else -> error("Unsupported blockchain: $blockchain")
        }
    }
}