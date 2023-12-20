package com.tangem.blockchain.common.address

import android.content.Context
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.bip39.*
import com.tangem.crypto.hdWallet.masterkey.AnyMasterKeyFactory
import com.tangem.sdk.extensions.getWordlist

class EstimationFeeAddressFactory {

    fun makeAddress(blockchain: Blockchain, context: Context): String {
        return when (blockchain) {
            Blockchain.Cardano -> {
                "addr1q9svm389hgtksjvawpt9nfd9twk4kfckhs23wxrdfspynw9g3emv6k6njzwqvdmtff4426vy2pfg0ngu9t6pr9xmd0ass48agt"
            }

            Blockchain.Chia,
            Blockchain.ChiaTestnet,
            -> {
                // Can not generate and doesn't depend on destination
                ""
            }

            Blockchain.XRP,
            Blockchain.Stellar,
            Blockchain.StellarTestnet,
            Blockchain.Binance,
            Blockchain.BinanceTestnet,
            Blockchain.Solana,
            Blockchain.SolanaTestnet,
            -> {
                // Doesn't depend on amount and destination
                ""
            }

            Blockchain.Tezos -> {
                // Tezos has a fixed fee. 
                ""
            }

            Blockchain.Kaspa -> {
                // Doesn't depend on destination
                ""
            }

            Blockchain.Ducatus, Blockchain.Unknown -> {
                // Unsupported
                ""
            }

            // We have to generate a new dummy address for

            // UTXO-like
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Litecoin,
            Blockchain.BitcoinCash,
            Blockchain.BitcoinCashTestnet,
            Blockchain.Dogecoin,
            Blockchain.Dash,
            Blockchain.Ravencoin,
            Blockchain.RavencoinTestnet,
                // EVM-like
            Blockchain.Ethereum,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumPow,
            Blockchain.EthereumPowTestnet,
            Blockchain.EthereumFair,
            Blockchain.EthereumClassic,
            Blockchain.EthereumClassicTestnet,
            Blockchain.RSK,
            Blockchain.BSC,
            Blockchain.BSCTestnet,
            Blockchain.Polygon,
            Blockchain.PolygonTestnet,
            Blockchain.Avalanche,
            Blockchain.AvalancheTestnet,
            Blockchain.Fantom,
            Blockchain.FantomTestnet,
            Blockchain.Arbitrum,
            Blockchain.ArbitrumTestnet,
            Blockchain.Gnosis,
            Blockchain.Optimism,
            Blockchain.OptimismTestnet,
            Blockchain.Kava,
            Blockchain.KavaTestnet,
            Blockchain.Cronos,
            Blockchain.Telos,
            Blockchain.TelosTestnet,
            Blockchain.OctaSpace,
            Blockchain.OctaSpaceTestnet,
            Blockchain.Decimal,
            Blockchain.DecimalTestnet,
                // Polkadot-like
            Blockchain.Polkadot,
            Blockchain.PolkadotTestnet,
            Blockchain.Kusama,
            Blockchain.AlephZero,
            Blockchain.AlephZeroTestnet,
                // Cosmos-like
            Blockchain.Cosmos,
            Blockchain.CosmosTestnet,
            Blockchain.TerraV1,
            Blockchain.TerraV2,
                // Others
            Blockchain.Tron,
            Blockchain.TronTestnet,
            Blockchain.TON,
            Blockchain.TONTestnet,
            Blockchain.Near,
            Blockchain.NearTestnet,
            -> {
                generateAddress(blockchain, context)
            }

        }
    }

    private fun generateAddress(blockchain: Blockchain, context: Context): String {
        val primaryCurve = primaryCurve(blockchain) ?: return ""
        val mnemonic = DefaultMnemonic(
            EntropyLength.Bits128Length,
            Wordlist.getWordlist(context)
        )

        val factory = AnyMasterKeyFactory(mnemonic, "")
        val masterKey = factory.makeMasterKey(primaryCurve)
        val extendedPublicKey = masterKey.makePublicKey(primaryCurve)
        val publicKey = extendedPublicKey.publicKey
        val addresses = blockchain.makeAddresses(
            walletPublicKey = publicKey,
            curve = primaryCurve,
        )
        return addresses.find { it.type == AddressType.Default }?.value ?: addresses.first().value
    }

    private fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        // order is important, new curve is preferred for wallet 2
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519Slip0010) -> {
                EllipticCurve.Ed25519Slip0010
            }

            blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }

            blockchain.getSupportedCurves().contains(EllipticCurve.Bls12381G2Aug) -> {
                EllipticCurve.Bls12381G2Aug
            }
            // only for support cardano on Wallet2
            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }

            else -> {
                null
            }
        }
    }
}