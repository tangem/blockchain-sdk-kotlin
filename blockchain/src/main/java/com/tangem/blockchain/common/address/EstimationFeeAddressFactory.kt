package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.masterkey.AnyMasterKeyFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * This entity provides addresses for calculating commissions in swap scenarios.
 *
 * If the blockchain lacks an "initialized account state", it generates a random address.
 *
 * If the commission for sending tokens to a “warmed-up” account differs from that for sending to an empty account,
 * the address of the “warmed-up” account should be set (e.g., in Cardano).
 *
 * If the fee does not depend on the amount or destination, or if the fee is fixed, an empty address can be set to
 * avoid wasting resources on address generation.
 *
 * @param mnemonic to help generate addresses
 */
class EstimationFeeAddressFactory(
    mnemonic: Mnemonic,
) {

    private val anyKeyMasterFactory = AnyMasterKeyFactory(
        mnemonic = mnemonic,
        passphrase = "",
    )

    private val addressesCache: ConcurrentHashMap<Blockchain, String> = ConcurrentHashMap()

    @Suppress("LongMethod")
    fun makeAddress(blockchain: Blockchain): String {
        return when (blockchain) {
            Blockchain.Cardano -> CARDANO_ESTIMATION_ADDRESS

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

            // We have to generate a new dummy address for UTXO-like
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Litecoin,
            Blockchain.BitcoinCash,
            Blockchain.BitcoinCashTestnet,
            Blockchain.Dogecoin,
            Blockchain.Dash,
            Blockchain.Ravencoin,
            Blockchain.RavencoinTestnet,
            Blockchain.Solana,
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
            Blockchain.XDC,
            Blockchain.XDCTestnet,
            Blockchain.VeChain,
            Blockchain.VeChainTestnet,
            Blockchain.Aptos,
            Blockchain.AptosTestnet,
            -> {
                generateAddress(blockchain)
            }
        }
    }

    private fun generateAddress(blockchain: Blockchain): String {
        val primaryCurve = primaryCurve(blockchain) ?: return ""

        val cachedValue = addressesCache[blockchain]

        if (cachedValue == null) {
            val masterKey = anyKeyMasterFactory.makeMasterKey(primaryCurve)
            val extendedPublicKey = masterKey.makePublicKey(primaryCurve)
            val publicKey = extendedPublicKey.publicKey
            val addresses = blockchain.makeAddresses(
                walletPublicKey = publicKey,
                curve = primaryCurve,
            )
            val address = addresses.find { it.type == AddressType.Default }?.value ?: addresses.first().value

            addressesCache[blockchain] = address

            return address
        } else {
            return cachedValue
        }
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

    companion object {
        private const val CARDANO_ESTIMATION_ADDRESS =
            "addr1q9svm389hgtksjvawpt9nfd9twk4kfckhs23wxrdfspynw9g3emv6k6njzwqvdmtff4426vy2pfg0ngu9t6pr9xmd0ass48agt"
    }
}
