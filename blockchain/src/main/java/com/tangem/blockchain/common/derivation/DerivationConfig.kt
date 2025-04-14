package com.tangem.blockchain.common.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.DerivationPath

@Suppress("UnnecessaryAbstractClass")
abstract class DerivationConfig {

    abstract fun derivations(blockchain: Blockchain): Map<AddressType, DerivationPath>
}
