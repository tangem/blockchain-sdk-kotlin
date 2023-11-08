package com.tangem.blockchain_demo.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.Card
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Created by Anton Zhilenkov on 12/08/2022.
 */
private const val TEST_CARD_BATCH = "99FF"
private const val TEST_CARD_ID_STARTS_WITH = "FF99"
private val firstCardSeries = listOf("CB61", "CB64")
private val secondCardSeries = listOf("CB62", "CB65")

private val tangemNoteBatches = mapOf(
    "AB01" to Blockchain.Bitcoin,
    "AB02" to Blockchain.Ethereum,
    "AB03" to Blockchain.Cardano,
    "AB04" to Blockchain.Dogecoin,
    "AB05" to Blockchain.BSC,
    "AB06" to Blockchain.XRP,
    "AB07" to Blockchain.Bitcoin,
    "AB08" to Blockchain.Ethereum,
    "AB09" to Blockchain.Bitcoin,       // new batches for 3.34
    "AB10" to Blockchain.Ethereum,
    "AB11" to Blockchain.Bitcoin,
    "AB12" to Blockchain.Ethereum,
)

val Card.useOldStyleDerivation: Boolean
    get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"

val Card.derivationStyle: DerivationStyle?
    get() = if (!settings.isHDWalletAllowed) {
        null
    } else if (useOldStyleDerivation) {
        DerivationStyle.LEGACY
    } else {
        DerivationStyle.NEW
    }

val Card.isTestCard: Boolean
    get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)

fun Card.getTangemNoteBlockchain(): Blockchain? = tangemNoteBatches[batchId]

fun Card.isTangemTwins(): Boolean = when {
    firstCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
        TwinCardNumber.First
    }
    secondCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
        TwinCardNumber.Second
    }
    else -> {
        null
    }
} != null

fun Card.derivationParams(derivationPath: DerivationPath?): DerivationParams? {
    return derivationStyle?.let {
        when (derivationPath) {
            null -> DerivationParams.Default(it)
            else -> DerivationParams.Custom(derivationPath)
        }
    }
}

private enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);
}

