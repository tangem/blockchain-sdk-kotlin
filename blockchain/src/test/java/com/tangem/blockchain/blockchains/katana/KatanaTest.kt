package com.tangem.blockchain.blockchains.katana

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.common.isUTXO
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Tests for Katana blockchain support.
 *
 * Katana is an Ethereum L2 (Polygon CDK / AggLayer) with ETH as its native coin, so every assertion
 * here also guards the properties that make an unsupported-network recovery possible: the address a
 * Tangem card derives for Katana must be the same address it derives for Ethereum, and the chain ID
 * used for EIP-155 signing must be Katana's and no other network's.
 */
internal class KatanaTest {

    @Test
    fun `Katana creates EthereumWalletManager`() {
        val walletManager = createKatanaWalletManager()

        Truth.assertThat(walletManager).isInstanceOf(EthereumWalletManager::class.java)
        Truth.assertThat(walletManager?.wallet?.blockchain).isEqualTo(Blockchain.Katana)
    }

    @Test
    fun `Katana has correct chain ID`() {
        val katanaChain = Chain.entries.find { it.blockchain == Blockchain.Katana }

        Truth.assertThat(katanaChain?.id).isEqualTo(KATANA_CHAIN_ID)
        Truth.assertThat(Blockchain.Katana.getChainId()).isEqualTo(KATANA_CHAIN_ID)
    }

    /**
     * A wrong chain ID would produce EIP-155 signatures that are valid on another network, so the
     * value is pinned rather than derived from the enum it is meant to guard.
     */
    @Test
    fun `Katana chain ID is not shared with any other chain`() {
        val chainsWithSameId = Chain.entries.filter { it.id == KATANA_CHAIN_ID }

        Truth.assertThat(chainsWithSameId.map { it.blockchain }).containsExactly(Blockchain.Katana)
    }

    @Test
    fun `Katana supports EIP-1559`() {
        Truth.assertThat(Blockchain.Katana.isSupportEIP1559).isTrue()
    }

    @Test
    fun `Katana is an EVM blockchain and not UTXO`() {
        Truth.assertThat(Blockchain.Katana.isEvm()).isTrue()
        Truth.assertThat(Blockchain.Katana.isUTXO).isFalse()
    }

    @Test
    fun `Katana is an L2 Ethereum network`() {
        Truth.assertThat(Blockchain.Katana.isL2EthereumNetwork()).isTrue()
    }

    @Test
    fun `Katana has correct coin metadata`() {
        Truth.assertThat(Blockchain.Katana.currency).isEqualTo("ETH")
        Truth.assertThat(Blockchain.Katana.fullName).isEqualTo("Katana")
        Truth.assertThat(Blockchain.Katana.decimals()).isEqualTo(18)
    }

    @Test
    fun `Katana is a mainnet without a testnet counterpart`() {
        Truth.assertThat(Blockchain.Katana.isTestnet()).isFalse()
        Truth.assertThat(Blockchain.Katana.getTestnetVersion()).isNull()
    }

    @Test
    fun `Katana supports Secp256k1 curve`() {
        Truth.assertThat(Blockchain.Katana.getSupportedCurves()).contains(EllipticCurve.Secp256k1)
    }

    /**
     * Recovery of assets already sitting at a card's EVM address depends on Katana deriving that
     * same address, so the path must match Ethereum's in every derivation style.
     */
    @Test
    fun `Katana derives the same path as Ethereum in every derivation style`() {
        listOf(DerivationStyle.V1, DerivationStyle.V2, DerivationStyle.V3).forEach { style ->
            Truth.assertThat(Blockchain.Katana.derivationPath(style)?.rawPath)
                .isEqualTo(ETHEREUM_DERIVATION_PATH)
            Truth.assertThat(Blockchain.Katana.derivationPath(style)?.rawPath)
                .isEqualTo(Blockchain.Ethereum.derivationPath(style)?.rawPath)
        }
    }

    @Test
    fun `Katana derives the same address as Ethereum for the same public key`() {
        val publicKey = TEST_PUBLIC_KEY.hexToBytes()

        val katanaAddress = Blockchain.Katana.makeAddresses(publicKey).map { it.value }.toSet()
        val ethereumAddress = Blockchain.Ethereum.makeAddresses(publicKey).map { it.value }.toSet()

        Truth.assertThat(katanaAddress).isEqualTo(ethereumAddress)
    }

    private fun createKatanaWalletManager(): WalletManager? {
        return WalletManagerFactory(
            config = BlockchainSdkConfig(),
            blockchainProviderTypes = mapOf(
                Blockchain.Katana to listOf(ProviderType.Public(url = KATANA_PUBLIC_RPC)),
            ),
            blockchainDataStorage = object : BlockchainDataStorage {
                override suspend fun getOrNull(key: String): String? = null
                override suspend fun store(key: String, value: String) = Unit
                override suspend fun remove(key: String) = Unit
            },
            accountCreator = object : AccountCreator {
                override suspend fun createAccount(blockchain: Blockchain, walletPublicKey: ByteArray): Result<String> {
                    return Result.Success("account")
                }
            },
            featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false),
        ).createLegacyWalletManager(
            blockchain = Blockchain.Katana,
            walletPublicKey = TEST_PUBLIC_KEY.hexToBytes(),
            curve = EllipticCurve.Secp256k1,
        )
    }

    private companion object {
        const val KATANA_CHAIN_ID = 747474
        const val KATANA_PUBLIC_RPC = "https://rpc.katana.network"
        const val ETHEREUM_DERIVATION_PATH = "m/44'/60'/0'/0/0"
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}
