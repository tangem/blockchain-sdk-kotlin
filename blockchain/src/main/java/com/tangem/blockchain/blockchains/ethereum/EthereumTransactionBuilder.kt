@file:Suppress("EnumEntryNameCase")

package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.toDecompressedPublicKey
import java.math.BigInteger

@Suppress("LongParameterList")
class EthereumTransactionBuilder(
    walletPublicKey: ByteArray,
    private val blockchain: Blockchain,
) {
    private val walletPublicKey: ByteArray = walletPublicKey.toDecompressedPublicKey().sliceArray(1..64)

    fun buildToSign(transactionData: TransactionData, nonce: BigInteger?): CompiledEthereumTransaction? {
        return EthereumUtils.buildTransactionToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
        )
    }

    fun buildToSend(signature: ByteArray, transactionToSign: CompiledEthereumTransaction): ByteArray {
        return EthereumUtils.prepareTransactionToSend(signature, transactionToSign, walletPublicKey, blockchain)
    }
}

enum class Chain(val id: Int, val blockchain: Blockchain?) {
    Mainnet(id = 1, blockchain = Blockchain.Ethereum),
    Goerli(id = 5, blockchain = Blockchain.EthereumTestnet),
    Morden(id = 2, blockchain = null),
    Ropsten(id = 3, blockchain = null),
    Kovan(id = 42, blockchain = null),
    RskMainnet(id = 30, blockchain = Blockchain.RSK),
    RskTestnet(id = 31, blockchain = null),
    BscMainnet(id = 56, blockchain = Blockchain.BSC),
    BscTestnet(id = 97, blockchain = Blockchain.BSCTestnet),
    EthereumClassicMainnet(id = 61, blockchain = Blockchain.EthereumClassic),
    EthereumClassicTestnet(id = 6, blockchain = Blockchain.EthereumClassicTestnet),
    Gnosis(id = 100, blockchain = Blockchain.Gnosis),
    Geth_private_chains(id = 1337, blockchain = null),
    Avalanche(id = 43114, blockchain = Blockchain.Avalanche),
    AvalancheTestnet(id = 43113, blockchain = Blockchain.AvalancheTestnet),
    Arbitrum(id = 42161, blockchain = Blockchain.Arbitrum),
    ArbitrumTestnet(id = 421613, blockchain = Blockchain.ArbitrumTestnet),
    Polygon(id = 137, blockchain = Blockchain.Polygon),
    PolygonTestnet(id = 80001, blockchain = Blockchain.PolygonTestnet),
    Fantom(id = 250, blockchain = Blockchain.Fantom),
    FantomTestnet(id = 4002, blockchain = Blockchain.FantomTestnet),
    Optimism(id = 10, blockchain = Blockchain.Optimism),
    OptimismTestnet(id = 420, blockchain = Blockchain.OptimismTestnet),
    EthereumFair(id = 513100, blockchain = Blockchain.Dischain),
    EthereumPow(id = 10001, blockchain = Blockchain.EthereumPow),
    EthereumPowTestnet(id = 10002, blockchain = Blockchain.EthereumPowTestnet),
    Kava(id = 2222, blockchain = Blockchain.Kava),
    KavaTestnet(id = 2221, blockchain = Blockchain.KavaTestnet),
    Telos(id = 40, blockchain = Blockchain.Telos),
    TelosTestnet(id = 41, blockchain = Blockchain.TelosTestnet),
    Cronos(id = 25, blockchain = Blockchain.Cronos),
    OctaSpace(id = 800001, blockchain = Blockchain.OctaSpace),
    OctaSpaceTestnet(id = 800002, blockchain = Blockchain.OctaSpaceTestnet),
    Decimal(id = 75, blockchain = Blockchain.Decimal),
    DecimalTestnet(id = 202020, blockchain = Blockchain.DecimalTestnet),
    Xdc(id = 50, blockchain = Blockchain.XDC),
    XdcTestnet(id = 51, blockchain = Blockchain.XDCTestnet),
    Playa3ull(id = 3011, blockchain = Blockchain.Playa3ull),
    Shibarium(id = 109, blockchain = Blockchain.Shibarium),
    ShibariumTestnet(id = 157, blockchain = Blockchain.ShibariumTestnet),
    Aurora(id = 1313161554, blockchain = Blockchain.Aurora),
    AuroraTestnet(id = 1313161555, blockchain = Blockchain.AuroraTestnet),
    Areon(id = 463, blockchain = Blockchain.Areon),
    AreonTestnet(id = 462, blockchain = Blockchain.AreonTestnet),
    PulseChain(id = 369, blockchain = Blockchain.PulseChain),
    PulseChainTestnet(id = 943, blockchain = Blockchain.PulseChainTestnet),
    ZkSyncEra(id = 324, blockchain = Blockchain.ZkSyncEra),
    ZkSyncEraTestnet(id = 300, blockchain = Blockchain.ZkSyncEraTestnet),
    Moonbeam(id = 1284, blockchain = Blockchain.Moonbeam),
    MoonbeamTestnet(id = 1287, blockchain = Blockchain.MoonbeamTestnet),
    Manta(id = 169, blockchain = Blockchain.Manta),
    MantaTestnet(id = 3441005, blockchain = Blockchain.MantaTestnet),
    PolygonZkEVM(id = 1101, blockchain = Blockchain.PolygonZkEVM),
    PolygonZkEVMTestnet(id = 2442, blockchain = Blockchain.PolygonZkEVMTestnet),
    Base(id = 8453, blockchain = Blockchain.Base),
    BaseTestnet(id = 84532, blockchain = Blockchain.BaseTestnet),
    Moonriver(id = 1285, blockchain = Blockchain.Moonriver),
    MoonriverTestnet(id = 1287, blockchain = Blockchain.MoonriverTestnet),
    Mantle(id = 5000, blockchain = Blockchain.Mantle),
    MantleTestnet(id = 5001, blockchain = Blockchain.MantleTestnet),
    Flare(id = 14, blockchain = Blockchain.Flare),
    FlareTestnet(id = 114, blockchain = Blockchain.FlareTestnet),
    Taraxa(id = 841, blockchain = Blockchain.Taraxa),
    TaraxaTestnet(id = 842, blockchain = Blockchain.TaraxaTestnet),
    Blast(id = 81457, blockchain = Blockchain.Blast),
    BlastTestnet(id = 168587773, blockchain = Blockchain.BlastTestnet),
}