package com.tangem.blockchain.common

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.card.Card.BackupStatus
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignResponse

class CommonSigner(var cardId: String?, var initialMessage: Message?, var tangemSdk: TangemSdk) {

    fun sign(
        hashes: Array<ByteArray>,
        walletPublicKey: Wallet.PublicKey,
        backupStatus: BackupStatus,
        callback: CompletionCallback<SignResponse>
    ) {
        tangemSdk.sign(
            hashes = hashes,
            walletPublicKey = walletPublicKey.seedKey,
            cardId = cardId,
            cardBackupStatus = backupStatus,
            initialMessage = initialMessage,
            callback = callback
        )
    }
    fun sign(
        hash: ByteArray,
        walletPublicKey: Wallet.PublicKey,
        backupStatus: BackupStatus,
        callback: CompletionCallback<SignHashResponse>
    ) {
        tangemSdk.sign(
            hash = hash,
            walletPublicKey = walletPublicKey.seedKey,
            cardId = cardId,
            backupStatus = backupStatus,
            initialMessage = initialMessage,
            callback = callback
        )
    }
}