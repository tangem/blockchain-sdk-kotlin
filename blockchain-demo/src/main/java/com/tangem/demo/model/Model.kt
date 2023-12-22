package com.tangem.demo.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.Card
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.demo.extensions.derivationStyle
import com.tangem.demo.extensions.getTangemNoteBlockchain
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Created by Anton Zhilenkov on 12/08/2022.
 */
data class ScanResponse(
    val card: Card,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
    val productType: ProductType = ProductType.Wallet,
) : CommandResponse {

    fun getBlockchain(): Blockchain {
        if (productType == ProductType.Note) {
            return card.getTangemNoteBlockchain()
                ?: return Blockchain.Unknown
        }
        val blockchainName: String = walletData?.blockchain ?: return Blockchain.Unknown
        return Blockchain.fromId(blockchainName)
    }
}

enum class ProductType {
    Note, Twins, Wallet
}

typealias KeyWalletPublicKey = ByteArrayKey

data class BlockchainNetwork(
    val blockchain: Blockchain,
    val derivationPath: String?,
    val tokens: List<Token>,
) {
    constructor(blockchain: Blockchain, card: Card) : this(
        blockchain = blockchain,
        derivationPath = if (card.settings.isHDWalletAllowed) {
            blockchain.derivationPath(card.derivationStyle)?.rawPath
        } else {
            null
        },
        tokens = emptyList(),
    )
}
