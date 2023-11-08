package com.tangem.blockchain_demo.cardSdk

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain_demo.extensions.useOldStyleDerivation
import com.tangem.blockchain_demo.model.BlockchainNetwork
import com.tangem.blockchain_demo.model.ScanResponse
import com.tangem.blockchain_demo.scope
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 11/08/2022.
 */
class ScanCardAndDerive(
    private val blockchainsToDerive: List<Blockchain>
) : CardSessionRunnable<ScanResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<ScanResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        scope.launch {
            val derivations = collectDerivations(card)
            if (derivations.isEmpty() || !card.settings.isHDWalletAllowed) {
                val scanResponse = ScanResponse(card, session.environment.walletData, null)
                callback(CompletionResult.Success(scanResponse))
                return@launch
            }

            DeriveMultipleWalletPublicKeysTask(derivations).run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        val response = ScanResponse(
                            card = card,
                            walletData = session.environment.walletData,
                            derivedKeys = result.data.entries,
                            primaryCard = null
                        )
                        callback(CompletionResult.Success(response))
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private suspend fun collectDerivations(card: Card): Map<ByteArrayKey, List<DerivationPath>> {
        val blockchains = getBlockchainsToDerive(card)
        val derivations = mutableMapOf<ByteArrayKey, List<DerivationPath>>()

        blockchains.forEach { blockchain ->
            val curve = blockchain.blockchain.getPrimaryCurve()

            val wallet = card.wallets.firstOrNull { it.curve == curve } ?: return@forEach
            if (wallet.chainCode == null) return@forEach

            val key = wallet.publicKey.toMapKey()
            val path = blockchain.derivationPath?.let { DerivationPath(it) }
            if (path != null) {
                val addedDerivations = derivations[key]
                if (addedDerivations != null) {
                    derivations[key] = addedDerivations + path
                } else {
                    derivations[key] = listOf(path)
                }
            }
        }
        return derivations
    }

    private suspend fun getBlockchainsToDerive(card: Card): List<BlockchainNetwork> {
        val blockchains = mutableListOf<BlockchainNetwork>()
        if (card.settings.isHDWalletAllowed) {
            blockchains.addAll(blockchainsToDerive.map { BlockchainNetwork(it, card) })
        }
        if (!card.useOldStyleDerivation) {
            blockchains.removeAll(
                listOf(
                    Blockchain.BSC, Blockchain.BSCTestnet,
                    Blockchain.Polygon, Blockchain.PolygonTestnet,
                    Blockchain.RSK,
                    Blockchain.Fantom, Blockchain.FantomTestnet,
                    Blockchain.Avalanche, Blockchain.AvalancheTestnet,
                ).map { BlockchainNetwork(it, card) }
            )
        }

        return blockchains.distinct()
    }
}