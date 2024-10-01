package com.tangem.blockchain.common

import android.util.Log
import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.Config
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.InMemoryStorage
import com.tangem.operations.read.ReadCommand
import io.mockk.every
import io.mockk.mockkStatic
import org.bitcoinj.core.ECKey
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.junit.Test
import org.kethereum.crypto.CryptoAPI
import org.kethereum.extensions.toBigInteger
import org.kethereum.model.ADDRESS_LENGTH_IN_HEX
import java.util.Locale

internal class WalletManagerFactoryTest {

    private val ecDsaPublicKey = (
        "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321998CC2D042901C91D40601E79A641E1CB" +
            "CEBE7A2358BE6054E1B6E5D"
        ).hexToBytes()
    private val edDsaPublicKey = "E078212D58B2B9D0EDC9C936830D10081CD38B90C31778C56DFB1171027E294E".hexToBytes()

    private val accountCreator = object : AccountCreator {
        override suspend fun createAccount(blockchain: Blockchain, walletPublicKey: ByteArray): Result<String> {
            return Result.Success("account")
        }
    }

    @Test
    fun createBitcoinWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Bitcoin, EllipticCurve.Secp256k1)

        Truth.assertThat(walletManager).isInstanceOf(BitcoinWalletManager::class.java)
    }

    @Test
    fun createEthereumWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Ethereum, EllipticCurve.Secp256k1)

        Truth.assertThat(walletManager).isInstanceOf(EthereumWalletManager::class.java)
    }

    @Test
    fun createStellarWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Stellar, EllipticCurve.Ed25519)

        Truth.assertThat(walletManager).isInstanceOf(StellarWalletManager::class.java)
    }

    @Test
    fun createCardanoWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Cardano, EllipticCurve.Ed25519)

        Truth.assertThat(walletManager).isInstanceOf(CardanoWalletManager::class.java)
    }

    @Test
    fun createXrpWalletManager() {
        val secpWalletManager = makeWalletManager(Blockchain.XRP, EllipticCurve.Secp256k1)
        val edWalletManager = makeWalletManager(Blockchain.XRP, EllipticCurve.Ed25519)

        Truth.assertThat(secpWalletManager).isInstanceOf(XrpWalletManager::class.java)
        Truth.assertThat(edWalletManager).isInstanceOf(XrpWalletManager::class.java)
    }

    @Test
    fun createBinanceWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Binance, EllipticCurve.Secp256k1)

        Truth.assertThat(walletManager).isInstanceOf(BinanceWalletManager::class.java)
    }

    @Test
    fun createBitcoinCashWalletManager() {
        val walletManager = makeWalletManager(Blockchain.BitcoinCash, EllipticCurve.Secp256k1)

        Truth.assertThat(walletManager).isInstanceOf(BitcoinCashWalletManager::class.java)
    }

    @Test
    fun createLitecoinWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Litecoin, EllipticCurve.Secp256k1)

        Truth.assertThat(walletManager).isInstanceOf(LitecoinWalletManager::class.java)
    }

    @Test
    fun createTezosWalletManager() {
        val secpWalletManager = makeWalletManager(Blockchain.Tezos, EllipticCurve.Secp256k1)
        val edWalletManager = makeWalletManager(Blockchain.Tezos, EllipticCurve.Ed25519)

        Truth.assertThat(secpWalletManager).isInstanceOf(TezosWalletManager::class.java)
        Truth.assertThat(edWalletManager).isInstanceOf(TezosWalletManager::class.java)
    }

    @Test
    fun createDucatusWalletManager() {
        val walletManager = makeWalletManager(Blockchain.Ducatus, EllipticCurve.Secp256k1)

        Truth.assertThat(walletManager).isInstanceOf(DucatusWalletManager::class.java)
    }

    @Test
    fun createMultisigBitcoinWalletManager() {
        val data = "0108bb00000000000304200754414e47454d00020102800a322e3432642053444b000341040876bdec26b89bd2159a66" +
            "8b9af3d9fe86370f318717c92b8d6c1186fb3648c32a5f9321998cc2d042901c91d40601e79a641e1cbcebe7a2358be6054e1b6" +
            "e5d0a04041e76310c618102ffff8a0101820407e30b0d830b54414e47454d2053444b0084034254438640e17ceec48c5be36240" +
            "c98019f95ad8b6e56acfebe60d11979c6279f715d607d76a860a137da8d109e805753f3f56b0130709f4bbf4cb9974b4c57b846" +
            "9bf4b873041045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f361d14e" +
            "6dc3f11b7d4ea183250a60720ebdf9e110cd26050a736563703235366b310008040000006407010009020bb8604104752a727e1" +
            "4bba5bd73b6714d72500f61ffd11026ad1196d2e1c54577cbeeac3d11fc68a64700f8d533f4e311964ea8fb3aa26c588295f213" +
            "3868d69c3e62869362040000005c6304000000090f01009000"

        val pairPublicKey = "04752A727E14BBA5BD73B6714D72500F61FFD11026AD1196D2E1C54577CBEEAC3D11FC68A64700F8D533F4E" +
            "311964EA8FB3AA26C588295F2133868D69C3E628693"

        val responseApdu = ResponseApdu(data.hexToBytes())
        val card = ReadCommand().deserialize(SessionEnvironment(Config(), InMemoryStorage()), responseApdu).card
        val config = BlockchainSdkConfig(nowNodeCredentials = NowNodeCredentials(apiKey = "4y821489124"))
        val walletManager = WalletManagerFactory(
            config = config,
            blockchainProviderTypes = BLOCKCHAIN_PROVIDER_TYPES,
            blockchainDataStorage = object : BlockchainDataStorage {
                override suspend fun getOrNull(key: String): String? = null
                override suspend fun store(key: String, value: String) = Unit
            },
            accountCreator = accountCreator,
            featureToggles = BlockchainFeatureToggles(isEthereumEIP1559Enabled = true),
        ).createTwinWalletManager(
            walletPublicKey = card.wallets.first().publicKey,
            pairPublicKey = pairPublicKey.hexToBytes(),
        )

        Truth.assertThat(walletManager).isInstanceOf(BitcoinWalletManager::class.java)
    }

    private fun makeWalletManager(blockchain: Blockchain, curve: EllipticCurve): WalletManager? {
        val publicKey = when (curve) {
            EllipticCurve.Secp256k1, EllipticCurve.Secp256r1 -> ecDsaPublicKey
            EllipticCurve.Ed25519 -> edDsaPublicKey
            else -> error("unsupported curve $curve")
        }

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        return WalletManagerFactory(
            BlockchainSdkConfig(
                blockchairCredentials = BlockchairCredentials(
                    apiKey = listOf("anyKey"),
                    authToken = "anyToken",
                ),
                blockcypherTokens = setOf(),
                infuraProjectId = "infuraProjectId",
                nowNodeCredentials = NowNodeCredentials(apiKey = "nowNodeCredentials"),
                getBlockCredentials = GetBlockCredentials(
                    xrp = GetBlockAccessToken(),
                    cardano = GetBlockAccessToken(),
                    avalanche = GetBlockAccessToken(),
                    eth = GetBlockAccessToken(),
                    etc = GetBlockAccessToken(),
                    fantom = GetBlockAccessToken(),
                    rsk = GetBlockAccessToken(),
                    bsc = GetBlockAccessToken(),
                    polygon = GetBlockAccessToken(),
                    gnosis = GetBlockAccessToken(),
                    cronos = GetBlockAccessToken(),
                    solana = GetBlockAccessToken(),
                    ton = GetBlockAccessToken(),
                    tron = GetBlockAccessToken(),
                    cosmos = GetBlockAccessToken(),
                    near = GetBlockAccessToken(),
                    dogecoin = GetBlockAccessToken(),
                    litecoin = GetBlockAccessToken(),
                    dash = GetBlockAccessToken(),
                    bitcoin = GetBlockAccessToken(),
                    aptos = GetBlockAccessToken(),
                    algorand = GetBlockAccessToken(),
                    zkSyncEra = GetBlockAccessToken(),
                    polygonZkEvm = GetBlockAccessToken(),
                    base = GetBlockAccessToken(),
                    blast = GetBlockAccessToken(),
                    filecoin = GetBlockAccessToken(),
                    sui = GetBlockAccessToken(),
                ),
                tronGridApiKey = "",
                chiaFireAcademyApiKey = "",
            ),
            blockchainProviderTypes = BLOCKCHAIN_PROVIDER_TYPES,
            blockchainDataStorage = object : BlockchainDataStorage {
                override suspend fun getOrNull(key: String): String? = null
                override suspend fun store(key: String, value: String) = Unit
            },
            accountCreator = accountCreator,
            featureToggles = BlockchainFeatureToggles(isEthereumEIP1559Enabled = true),
        ).createLegacyWalletManager(
            blockchain,
            publicKey,
            curve,
        )
    }

    @Test
    fun test() {
        val ecParams = ECKey.CURVE
        val privateKey = "AF2B9E5C9FE06DC2122D0FDE2433BCE6E72C6AB58BDCF4A25F5FFC7F556006E1".hexToBytes().toBigInteger()
        val privateKeyParameters = ECPrivateKeyParameters(privateKey, ecParams)
        val publicKeyPoint = privateKeyParameters.parameters.g.multiply(privateKeyParameters.d)
        val publicKeyBytes = publicKeyPoint.getEncoded(false)
        val publicKeyHash = Keccak.Digest256().digest(publicKeyBytes).toHexString()
        val ethereumAddress = publicKeyHash.substring(publicKeyHash.length - ADDRESS_LENGTH_IN_HEX)
        println(ethereumAddress.lowercase(Locale.getDefault()))

        val hash = "95DB218B4288E60DA2807FC42EFBB2BFEF0A19607EC1D701AF54704DC5A253F6".hexToBytes()
        val signature = CryptoAPI.signer.sign(hash, privateKey, true)
        val signatureBytes = signature.r.toByteArray() + signature.s.toByteArray()

        println(publicKeyBytes.toHexString())
        println(signatureBytes.toHexString())
    }

    private companion object {

        val BLOCKCHAIN_PROVIDER_TYPES = mapOf(
            Blockchain.BitcoinCash to listOf(ProviderType.NowNodes),
            Blockchain.Cardano to listOf(ProviderType.Cardano.Adalite),
            Blockchain.Tezos to listOf(ProviderType.Public(url = "url")),
            Blockchain.Stellar to listOf(ProviderType.NowNodes),
            Blockchain.Litecoin to listOf(ProviderType.NowNodes),
            Blockchain.Ethereum to listOf(ProviderType.NowNodes),
            Blockchain.Bitcoin to listOf(ProviderType.NowNodes),
            Blockchain.XRP to listOf(ProviderType.NowNodes),
        )
    }
}