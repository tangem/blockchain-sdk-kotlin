package com.tangem.demo.extensions

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.demo.model.BlockchainNetwork
import com.tangem.demo.model.ScanResponse

/**
 * Created by Anton Zhilenkov on 19/08/2022.
 */
@Deprecated("Need refactor to use right derivations")
fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse,
    blockchain: Blockchain,
    derivationParams: DerivationParams?,
): WalletManager? {
    val card = scanResponse.card
    if (card.isTestCard && blockchain.getTestnetVersion() == null) return null
    val supportedCurves = blockchain.getSupportedCurves()

    val wallets = card.wallets.filter { wallet -> supportedCurves.contains(wallet.curve) }
    val wallet = selectWallet(wallets) ?: return null

    val environmentBlockchain = if (card.isTestCard) blockchain.getTestnetVersion()!! else blockchain

    val seedKey = wallet.extendedPublicKey
    return when {
        scanResponse.card.isTangemTwins() && scanResponse.secondTwinPublicKey != null -> {
            createTwinWalletManager(
                walletPublicKey = wallet.publicKey,
                pairPublicKey = scanResponse.secondTwinPublicKey.hexToBytes(),
                blockchain = environmentBlockchain,
                curve = wallet.curve,
            )
        }

        seedKey != null && derivationParams != null -> {
            val derivedKeys = scanResponse.derivedKeys[wallet.publicKey.toMapKey()]
            val derivationPath = when (derivationParams) {
                is DerivationParams.Default -> blockchain.derivationPath(derivationParams.style)
                is DerivationParams.Custom -> derivationParams.path
            }
            val derivedKey = derivedKeys?.get(derivationPath)
                ?: return null

            val hdKey = derivationPath?.let { derivationPath ->
                Wallet.PublicKey.DerivationType.Plain(
                    Wallet.HDKey(
                        extendedPublicKey = derivedKey,
                        path = derivationPath,
                    ),
                )
            }

            createWalletManager(
                blockchain = environmentBlockchain,
                publicKey = Wallet.PublicKey(
                    seedKey = wallet.publicKey,
                    derivationType = hdKey,
                ),
                curve = wallet.curve,
            )
        }

        else -> {
            createLegacyWalletManager(
                blockchain = environmentBlockchain,
                walletPublicKey = wallet.publicKey,
                curve = wallet.curve,
            )
        }
    }
}

fun WalletManagerFactory.makeWalletManagerForApp(
    scanResponse: ScanResponse,
    blockchainNetwork: BlockchainNetwork,
): WalletManager? {
    return makeWalletManagerForApp(
        scanResponse,
        blockchain = blockchainNetwork.blockchain,
        derivationParams = getDerivationParams(blockchainNetwork.derivationPath, scanResponse.card),
    )
}

private fun getDerivationParams(derivationPath: String?, card: Card): DerivationParams? {
    return derivationPath?.let {
        DerivationParams.Custom(
            DerivationPath(it),
        )
    } ?: if (!card.settings.isHDWalletAllowed) {
        null
    } else if (card.useOldStyleDerivation) {
        DerivationParams.Default(DerivationStyle.LEGACY)
    } else {
        DerivationParams.Default(DerivationStyle.NEW)
    }
}

fun WalletManagerFactory.makeWalletManagersForApp(
    scanResponse: ScanResponse,
    blockchains: List<BlockchainNetwork>,
): List<WalletManager> {
    return blockchains.mapNotNull { this.makeWalletManagerForApp(scanResponse, it) }
}

private fun selectWallet(wallets: List<CardWallet>): CardWallet? {
    return when (wallets.size) {
        0 -> null
        1 -> wallets[0]
        else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
    }
}
