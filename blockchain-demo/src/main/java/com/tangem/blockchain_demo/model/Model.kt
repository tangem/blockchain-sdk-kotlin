package com.tangem.blockchain_demo.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain_demo.extensions.derivationStyle
import com.tangem.common.card.Card
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
[REDACTED_AUTHOR]
 */
data class ScanResponse(
    val card: Card,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null
) : CommandResponse

typealias KeyWalletPublicKey = ByteArrayKey

data class BlockchainNetwork(
    val blockchain: Blockchain,
    val derivationPath: String?,
    val tokens: List<Token>
) {
    constructor(blockchain: Blockchain, card: Card) : this(
        blockchain = blockchain,
        derivationPath = if (card.settings.isHDWalletAllowed) blockchain.derivationPath(card.derivationStyle)?.rawPath else null,
        tokens = emptyList()
    )
}