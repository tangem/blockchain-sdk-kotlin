package com.tangem.blockchain.blockchains.cardano

import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

@Suppress("MagicNumber")
object CardanoUtils {

    // Link to the original code:
    // https://github.com/trustwallet/wallet-core/blob/aa7475536e8c5b0383b1553073139b3498a9e35f/src/HDWallet.cpp#L163
    fun extendedDerivationPath(derivationPath: DerivationPath): DerivationPath {
        val nodes = derivationPath.nodes.toMutableList()
        if (nodes.size != 5) {
            error("Derivation path is short")
        }

        nodes[3] = DerivationNode.NonHardened(2)
        nodes[4] = DerivationNode.NonHardened(0)

        return DerivationPath(nodes)
    }

    // Method for computing an extended public key
    fun extendPublicKey(publicKey: ExtendedPublicKey, extendedPublicKey: ExtendedPublicKey): ByteArray {
        return publicKey.publicKey + publicKey.chainCode + extendedPublicKey.publicKey + extendedPublicKey.chainCode
    }
}
