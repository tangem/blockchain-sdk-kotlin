package com.tangem.blockchain.blockchains.ethereum

import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

/**
 * Represent datum which provided after specific derivation - address, path, publicKey.
 */
data class EthereumDerivationData(
    val address: String,
    val path: DerivationPath?,
    val publicKey: ExtendedPublicKey,
    val index: Int?,
)